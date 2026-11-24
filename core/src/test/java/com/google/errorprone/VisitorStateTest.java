/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.collect.Streams.concat;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.tools.StandardLocation.ANNOTATION_PROCESSOR_PATH;

import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.testing.GcFinalization;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link VisitorState}. */
@RunWith(JUnit4.class)
public class VisitorStateTest {

  @Test
  public void symbolFromString_defaultPackage() {
    assertThat(VisitorState.inferBinaryName("InDefaultPackage")).isEqualTo("InDefaultPackage");
  }

  @Test
  public void symbolFromString_nestedTypeInDefaultPackage() {
    assertThat(VisitorState.inferBinaryName("InDefaultPackage.Nested"))
        .isEqualTo("InDefaultPackage$Nested");
  }

  @Test
  public void symbolFromString_regularClass() {
    assertThat(VisitorState.inferBinaryName("test.RegularClass")).isEqualTo("test.RegularClass");
    assertThat(VisitorState.inferBinaryName("com.google.RegularClass"))
        .isEqualTo("com.google.RegularClass");
  }

  @Test
  public void symbolFromString_nestedTypeInRegularPackage() {
    assertThat(VisitorState.inferBinaryName("test.RegularClass.Nested"))
        .isEqualTo("test.RegularClass$Nested");
    assertThat(VisitorState.inferBinaryName("com.google.RegularClass.Nested"))
        .isEqualTo("com.google.RegularClass$Nested");
  }

  @Test
  public void getConstantExpression() {
    JavacTask task =
        JavacTool.create()
            .getTask(
                /* out= */ null,
                FileManagers.testFileManager(),
                /* diagnosticListener= */ null,
                /* options= */ ImmutableList.of(),
                /* classes= */ ImmutableList.of(),
                /* compilationUnits= */ ImmutableList.of());
    Context context = ((BasicJavacTask) task).getContext();
    VisitorState visitorState = VisitorState.createForUtilityPurposes(context);
    assertThat(visitorState.getConstantExpression("hello ' world")).isEqualTo("\"hello ' world\"");
    assertThat(visitorState.getConstantExpression("hello \n world"))
        .isEqualTo("\"hello \\n world\"");
    assertThat(visitorState.getConstantExpression('\'')).isEqualTo("'\\''");
  }

  // The following is taken from ErrorProneJavacPluginTest. There may be an easier way.
  // It's possible that it's overkill for what we need here.

  /** A bugpattern for testing. */
  @BugPattern(summary = "", severity = ERROR)
  public static class CheckThatTriesToMemoizeBasedOnTreePath extends BugChecker
      implements ClassTreeMatcher {
    // The following is bogus: The value is cached per compilation, not per compilation unit!
    // We'll test that the plugin throws an exception about that problem.
    private final Supplier<Optional<ClassTree>> firstClassTreeInCompilationUnit =
        VisitorState.memoize(s ->
            s.getPath().getCompilationUnit().getTypeDecls().stream()
                .filter(t -> t.getKind().asInterface().equals(ClassTree.class))
                .map(ClassTree.class::cast)
                .findFirst());

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(
              "Found "
                  + tree.getSimpleName()
                  + " in file whose main class is "
                  + firstClassTreeInCompilationUnit.get(state).map(ClassTree::getSimpleName))
          .build();
    }
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void memoizeCannotAccessTreePath() throws IOException {
    File libJar = tempFolder.newFile("lib.jar");
    try (var fis = new FileOutputStream(libJar);
        var jos = new JarOutputStream(fis)) {
      jos.putNextEntry(new JarEntry("META-INF/services/" + BugChecker.class.getName()));
      jos.write(CheckThatTriesToMemoizeBasedOnTreePath.class.getName().getBytes(UTF_8));
    }

    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(
        source,
        ImmutableList.of(
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test {",
            "  void f() {",
            "    return;",
            "  }",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    fileManager.setLocation(
        ANNOTATION_PROCESSOR_PATH,
        concat(
                Stream.of(libJar),
                Splitter.on(File.pathSeparatorChar)
                    .splitToStream(StandardSystemProperty.JAVA_CLASS_PATH.value())
                    .map(File::new))
            .collect(toImmutableList()));
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of(
                    "-Xplugin:ErrorProne -XepDisableAllChecks"
                        + " -Xep:CheckThatTriesToMemoizeBasedOnTreePath:ERROR",
                    "-XDcompilePolicy=byfile",
                    "--should-stop=ifError=FLOW",
                    "-XDaddTypeAnnotationsToSymbol=true"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    assertThat(task.call()).isFalse();
    Diagnostic<? extends JavaFileObject> diagnostic =
        diagnosticCollector.getDiagnostics().stream()
            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
            .collect(onlyElement());
    assertThat(diagnostic.getMessage(ENGLISH)).contains("UnsupportedOperationException");
  }

  /** A bugpattern that memoizes a value, like checks that keep matchers in static fields. */
  @BugPattern(summary = "", severity = ERROR)
  public static class CheckThatMemoizes extends BugChecker implements ClassTreeMatcher {
    static final AtomicInteger computations = new AtomicInteger();
    static volatile WeakReference<Object> lastValue;

    private static final Supplier<Object> VALUE =
        VisitorState.memoize(
            s -> {
              computations.incrementAndGet();
              Object value = new Object();
              lastValue = new WeakReference<>(value);
              return value;
            });

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      Object unused = VALUE.get(state);
      return Description.NO_MATCH;
    }
  }

  private static void compileWithCheckThatMemoizes() {
    CompilationTestHelper.newInstance(CheckThatMemoizes.class, VisitorStateTest.class)
        .addSourceLines("A.java", "class A {}")
        .addSourceLines("B.java", "class B {}")
        .expectNoDiagnostics()
        .doTest();
  }

  @Test
  public void memoize_reusesValueAcrossCompilationUnits() {
    CheckThatMemoizes.computations.set(0);
    compileWithCheckThatMemoizes();
    assertThat(CheckThatMemoizes.computations.get()).isEqualTo(1);
  }

  @Test
  public void memoize_computesValueForEachCompilation() {
    CheckThatMemoizes.computations.set(0);
    compileWithCheckThatMemoizes();
    compileWithCheckThatMemoizes();
    assertThat(CheckThatMemoizes.computations.get()).isEqualTo(2);
  }

  @Test
  public void memoize_doesNotRetainValueAfterCompilation() {
    compileWithCheckThatMemoizes();
    GcFinalization.awaitClear(CheckThatMemoizes.lastValue);
  }

  private static VisitorState newState() {
    Context context = new Context();
    JavacFileManager.preRegister(context);
    return VisitorState.createForUtilityPurposes(context);
  }

  @Test
  public void memoize_keepsValueWhileCompilationIsAlive() {
    AtomicInteger computations = new AtomicInteger();
    Supplier<Object> supplier =
        VisitorState.memoize(
            s -> {
              computations.incrementAndGet();
              return new Object(); // owned by nothing but the cache
            });
    VisitorState state = newState();
    try {
      WeakReference<Object> value = new WeakReference<>(supplier.get(state));
      GcFinalization.awaitFullGc();
      assertThat(value.get()).isNotNull();
      assertThat(supplier.get(state)).isSameInstanceAs(value.get());
      assertThat(computations.get()).isEqualTo(1);
    } finally {
      Reference.reachabilityFence(state);
    }
  }

  @Test
  public void memoize_keepsValuesOfInterleavedCompilations() {
    AtomicInteger computations = new AtomicInteger();
    Supplier<Object> supplier =
        VisitorState.memoize(
            s -> {
              computations.incrementAndGet();
              return new Object();
            });
    VisitorState a = newState();
    VisitorState b = newState();
    Object fromA = supplier.get(a);
    Object fromB = supplier.get(b);
    assertThat(fromB).isNotSameInstanceAs(fromA);
    assertThat(supplier.get(a)).isSameInstanceAs(fromA);
    assertThat(supplier.get(b)).isSameInstanceAs(fromB);
    assertThat(computations.get()).isEqualTo(2);
  }

  @Test
  public void memoize_retriesNullResults() {
    AtomicInteger computations = new AtomicInteger();
    Object later = new Object();
    Supplier<Object> supplier =
        VisitorState.memoize(s -> computations.incrementAndGet() < 3 ? null : later);
    VisitorState state = newState();
    assertThat(supplier.get(state)).isNull();
    assertThat(supplier.get(state)).isNull();
    assertThat(supplier.get(state)).isSameInstanceAs(later);
    assertThat(supplier.get(state)).isSameInstanceAs(later);
    assertThat(computations.get()).isEqualTo(3);
  }

  @Test
  public void memoize_computesOneValueAtATime() throws Exception {
    CountDownLatch firstEntered = new CountDownLatch(1);
    CountDownLatch secondEntered = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    Supplier<Object> supplier =
        VisitorState.memoize(
            s -> {
              if (calls.incrementAndGet() == 1) {
                firstEntered.countDown();
                Uninterruptibles.awaitUninterruptibly(releaseFirst, 10, SECONDS);
              } else {
                secondEntered.countDown();
              }
              return new Object();
            });
    VisitorState a = newState();
    VisitorState b = newState();
    FutureTask<Object> first = new FutureTask<>(() -> supplier.get(a));
    FutureTask<Object> second = new FutureTask<>(() -> supplier.get(b));
    Thread secondThread = new Thread(second);
    try {
      new Thread(first).start();
      assertThat(firstEntered.await(10, SECONDS)).isTrue();
      secondThread.start();
      // While the first computation is still running, wait until the second one either starts
      // (computations are not serialized) or blocks on the memoized supplier's monitor.
      long deadline = System.nanoTime() + SECONDS.toNanos(10);
      while (secondEntered.getCount() > 0 && !blockedOnMemoizedSupplier(secondThread)) {
        assertWithMessage("second computation neither started nor blocked")
            .that(System.nanoTime() < deadline)
            .isTrue();
        Thread.sleep(1);
      }
      assertWithMessage("second computation started while the first was running")
          .that(secondEntered.getCount())
          .isEqualTo(1);
    } finally {
      releaseFirst.countDown();
    }
    assertThat(first.get(10, SECONDS)).isNotSameInstanceAs(second.get(10, SECONDS));
    assertThat(calls.get()).isEqualTo(2);
  }

  private static boolean blockedOnMemoizedSupplier(Thread thread) {
    ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(thread.threadId());
    return info != null
        && info.getThreadState() == Thread.State.BLOCKED
        && info.getLockInfo() != null
        && info.getLockInfo().getClassName().equals(VisitorState.class.getName() + "$Cache");
  }
}
