/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.DiagnosticTestHelper.DIAGNOSTIC_CONTAINING;
import static com.google.errorprone.FileObjects.forResources;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.Finally;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Constants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class ErrorProneJavaCompilerTest {

  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void isSupportedOption() {
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler();

    // javac options should be passed through
    assertThat(compiler.isSupportedOption("-source")).isEqualTo(1);

    // error-prone options should be handled
    assertThat(compiler.isSupportedOption("-Xep:")).isEqualTo(0);
    assertThat(compiler.isSupportedOption("-XepIgnoreUnknownCheckNames")).isEqualTo(0);
    assertThat(compiler.isSupportedOption("-XepDisableWarningsInGeneratedCode")).isEqualTo(0);

    // old-style error-prone options are not supported
    assertThat(compiler.isSupportedOption("-Xepdisable:")).isEqualTo(-1);
  }

  interface JavaFileObjectDiagnosticListener extends DiagnosticListener<JavaFileObject> {}

  @Test
  public void getStandardJavaFileManager() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    JavaFileObjectDiagnosticListener listener = mock(JavaFileObjectDiagnosticListener.class);
    Locale locale = Locale.CANADA;

    var unused = compiler.getStandardFileManager(listener, locale, null);
    verify(mockCompiler).getStandardFileManager(listener, locale, null);
  }

  @Test
  public void run() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    InputStream in = mock(InputStream.class);
    OutputStream out = mock(OutputStream.class);
    OutputStream err = mock(OutputStream.class);
    String[] arguments = {"-source", "8", "-target", "8"};

    var unused = compiler.run(in, out, err, arguments);
    verify(mockCompiler).run(in, out, err, arguments);
  }

  @Test
  public void sourceVersion() {
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler();
    assertThat(compiler.getSourceVersions()).contains(SourceVersion.latest());
    assertThat(compiler.getSourceVersions()).doesNotContain(SourceVersion.RELEASE_5);
  }

  @Test
  public void fileWithErrorIntegrationTest() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[SelfAssignment]");
  }

  @Test
  public void withDisabledCheck() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Arrays.asList("-Xep:SelfAssignment:OFF"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
  }

  @Test
  public void withCheckPromotedToError() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/WaitNotInLoopPositiveCases.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[WaitNotInLoop]");

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/WaitNotInLoopPositiveCases.java"),
            Arrays.asList("-Xep:WaitNotInLoop:ERROR"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[WaitNotInLoop]");
  }

  @Test
  public void withCheckDemotedToWarning() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[SelfAssignment]");

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Arrays.asList("-Xep:SelfAssignment:WARN"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[SelfAssignment]");
  }

  @Test
  public void withNonDefaultCheckOn() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/EmptyIfStatementPositiveCases.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics()).isEmpty();

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/EmptyIfStatementPositiveCases.java"),
            Arrays.asList("-Xep:EmptyIf"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[EmptyIf]");
  }

  @Test
  public void badFlagThrowsException() {
    try {
      doCompile(
          Arrays.asList("bugpatterns/testdata/EmptyIfStatementPositiveCases.java"),
          Arrays.asList("-Xep:foo:bar:baz"),
          Collections.<Class<? extends BugChecker>>emptyList());
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessageThat().contains("invalid flag");
    }
  }

  @BugPattern(
      name = "ArrayEquals",
      summary = "Reference equality used to compare arrays",
      explanation = "",
      severity = ERROR,
      disableable = false)
  public static class UnsuppressibleArrayEquals extends ArrayEquals {}

  @Test
  public void cantDisableNonDisableableCheck() {
    try {
      doCompile(
          Arrays.asList("bugpatterns/testdata/ArrayEqualsPositiveCases.java"),
          Arrays.asList("-Xep:ArrayEquals:OFF"),
          ImmutableList.<Class<? extends BugChecker>>of(UnsuppressibleArrayEquals.class));
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessageThat().contains("ArrayEquals may not be disabled");
    }
  }

  @Test
  public void withCustomCheckPositive() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/BadShiftAmountPositiveCases.java"),
            Collections.<String>emptyList(),
            Arrays.<Class<? extends BugChecker>>asList(BadShiftAmount.class));
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    assertThat(result.diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[BadShiftAmount]");
  }

  @Test
  public void withCustomCheckNegative() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Arrays.<Class<? extends BugChecker>>asList(Finally.class));
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @Test
  public void severityResetsAfterOverride() throws IOException {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    JavacFileManager fileManager = FileManagers.testFileManager();
    JavaCompiler errorProneJavaCompiler = new ErrorProneJavaCompiler();
    List<String> args =
        Lists.newArrayList(
            "-d",
            tempDir.getRoot().getAbsolutePath(),
            "-proc:none",
            "-Xep:ChainingConstructorIgnoresParameter:WARN");
    ImmutableList<JavaFileObject> sources =
        forResources(
            ChainingConstructorIgnoresParameter.class,
            "testdata/ChainingConstructorIgnoresParameterPositiveCases.java");

    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter, fileManager, diagnosticHelper.collector, args, null, sources);
    boolean succeeded = task.call();
    assertThat(succeeded).isTrue();
    assertThat(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[ChainingConstructorIgnoresParameter]");

    // reset state between compilations
    diagnosticHelper.clearDiagnostics();
    sources =
        forResources(
            ChainingConstructorIgnoresParameter.class,
            "testdata/ChainingConstructorIgnoresParameterPositiveCases.java");
    args.remove("-Xep:ChainingConstructorIgnoresParameter:WARN");

    task =
        errorProneJavaCompiler.getTask(
            printWriter, fileManager, diagnosticHelper.collector, args, null, sources);
    succeeded = task.call();
    assertThat(succeeded).isFalse();
    assertThat(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[ChainingConstructorIgnoresParameter]");
  }

  @Test
  public void maturityResetsAfterOverride() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    JavaCompiler errorProneJavaCompiler = new ErrorProneJavaCompiler();
    List<String> args =
        Lists.newArrayList("-d", tempDir.getRoot().getAbsolutePath(), "-proc:none", "-Xep:EmptyIf");
    ImmutableList<JavaFileObject> sources =
        forResources(BadShiftAmount.class, "testdata/EmptyIfStatementPositiveCases.java");

    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter, null, diagnosticHelper.collector, args, null, sources);
    boolean succeeded = task.call();
    assertThat(succeeded).isFalse();
    assertThat(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[EmptyIf]");

    diagnosticHelper.clearDiagnostics();
    args.remove("-Xep:EmptyIf");
    task =
        errorProneJavaCompiler.getTask(
            printWriter, null, diagnosticHelper.collector, args, null, sources);
    succeeded = task.call();
    assertThat(succeeded).isTrue();
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @BugPattern(
      summary =
          "You appear to be using methods; prefer to implement all program logic inside the main"
              + " function by flipping bits in a single long[].",
      explanation = "",
      severity = ERROR,
      disableable = false)
  public static class DeleteMethod extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      MethodTree ctor = (MethodTree) Iterables.getOnlyElement(tree.getMembers());
      Preconditions.checkArgument(ASTHelpers.isGeneratedConstructor(ctor));
      return describeMatch(tree, SuggestedFix.delete(ctor));
    }
  }

  @Test
  public void fixGeneratedConstructor() {
    CompilationResult result =
        doCompile(
            Arrays.asList("testdata/DeleteGeneratedConstructorTestCase.java"),
            Collections.<String>emptyList(),
            ImmutableList.<Class<? extends BugChecker>>of(DeleteMethod.class));
    assertThat(result.succeeded).isFalse();
    assertThat(result.output).isEmpty();
    assertThat(result.diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(
            Iterables.getOnlyElement(result.diagnosticHelper.getDiagnostics()).getMessage(ENGLISH))
        .contains("IllegalArgumentException: Cannot edit synthetic AST nodes");
  }

  @Test
  public void withExcludedPaths() {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Arrays.asList("-XepExcludedPaths:.*/bugpatterns/.*"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();

    // ensure regexp must match the full path
    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Arrays.asList("-XepExcludedPaths:bugpatterns"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
  }

  @BugPattern(summary = "Test bug pattern to test custom patch functionality", severity = ERROR)
  public static final class AssignmentUpdater extends BugChecker implements VariableTreeMatcher {
    private final String newValue;

    @Inject
    AssignmentUpdater(ErrorProneFlags flags) {
      newValue = flags.get("AssignmentUpdater:NewValue").orElse("flag-not-set");
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return describeMatch(
          tree, SuggestedFix.replace(tree.getInitializer(), Constants.format(newValue)));
    }
  }

  @Test
  public void patchWithBugPatternCustomization() throws IOException {
    // Patching modifies files on disk, so we must create an actual file that matches the
    // `SimpleJavaFileObject` defined below.
    Path location = tempDir.getRoot().toPath().resolve("StringConstantWrapper.java");
    String source =
        """
        class StringConstantWrapper {
          String s = "old-value";
        }
        """;
    Files.writeString(location, source);

    CompilationResult result =
        doCompile(
            Collections.singleton(
                new SimpleJavaFileObject(location.toUri(), SimpleJavaFileObject.Kind.SOURCE) {
                  @Override
                  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                  }
                }),
            Arrays.asList(
                "-XepPatchChecks:AssignmentUpdater",
                "-XepPatchLocation:IN_PLACE",
                "-XepOpt:AssignmentUpdater:NewValue=new-value"),
            Collections.singletonList(AssignmentUpdater.class));
    assertThat(result.succeeded).isTrue();
    assertThat(Files.readString(location))
        .isEqualTo(
            """
            class StringConstantWrapper {
              String s = "new-value";
            }
            """);
  }

  private static class CompilationResult {
    public final boolean succeeded;
    public final String output;
    public final DiagnosticTestHelper diagnosticHelper;

    public CompilationResult(
        boolean succeeded, String output, DiagnosticTestHelper diagnosticHelper) {
      this.succeeded = succeeded;
      this.output = output;
      this.diagnosticHelper = diagnosticHelper;
    }
  }

  private CompilationResult doCompile(
      List<String> fileNames,
      List<String> extraArgs,
      List<Class<? extends BugChecker>> customCheckers) {
    return doCompile(
        forResources(getClass(), fileNames.toArray(new String[0])), extraArgs, customCheckers);
  }

  private CompilationResult doCompile(
      Iterable<? extends JavaFileObject> files,
      List<String> extraArgs,
      List<Class<? extends BugChecker>> customCheckers) {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    JavacFileManager fileManager = FileManagers.testFileManager();

    List<String> args = Lists.newArrayList("-d", tempDir.getRoot().getAbsolutePath(), "-proc:none");
    args.addAll(extraArgs);

    JavaCompiler errorProneJavaCompiler =
        (customCheckers.isEmpty())
            ? new ErrorProneJavaCompiler()
            : new ErrorProneJavaCompiler(ScannerSupplier.fromBugCheckerClasses(customCheckers));
    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter, fileManager, diagnosticHelper.collector, args, null, files);

    return new CompilationResult(
        task.call(), new String(outputStream.toByteArray(), UTF_8), diagnosticHelper);
  }
}
