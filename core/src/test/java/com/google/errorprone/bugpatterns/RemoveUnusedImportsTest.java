/*
 * Copyright 2016 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author gak@google.com (Gregory Kick) */
@RunWith(JUnit4.class)
public class RemoveUnusedImportsTest {
  private BugCheckerRefactoringTestHelper testHelper;

  @Before
  public void setUp() {
    this.testHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveUnusedImports(), getClass());
  }

  @Test
  public void basicUsageTest() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import static java.util.Collections.emptyList;",
            "import static java.util.Collections.emptySet;",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "",
            "import java.util.ArrayList;",
            "import java.util.Collection;",
            "import java.util.Collections;",
            "import java.util.HashSet;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import java.util.UUID;",
            "public class Test {",
            "  private final Object object;",
            "",
            "  Test(Object object) {",
            "    this.object = checkNotNull(object);",
            "  }",
            "",
            "  Set<UUID> someMethod(Collection<UUID> collection) {",
            "    if (collection.isEmpty()) {",
            "      return emptySet();",
            "    }",
            "    return new HashSet<>(collection);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.util.Collections.emptySet;",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "",
            "import java.util.Collection;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "import java.util.UUID;",
            "public class Test {",
            "  private final Object object;",
            "",
            "  Test(Object object) {",
            "    this.object = checkNotNull(object);",
            "  }",
            "",
            "  Set<UUID> someMethod(Collection<UUID> collection) {",
            "    if (collection.isEmpty()) {",
            "      return emptySet();",
            "    }",
            "    return new HashSet<>(collection);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void useInSelect() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Map;",
            "import java.util.Map.Entry;",
            "public class Test {",
            "  Map.Entry<String, String> e;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Map;",
            "public class Test {",
            "  Map.Entry<String, String> e;",
            "}")
        .doTest();
  }

  @Test
  public void useInJavadocSee() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import java.util.Map;",
            "/** @see Map */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocSeeSelect() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import java.util.Map;",
            "/** @see Map#get */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocLink() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import java.util.Map;",
            "/** {@link Map} */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocLink_selfReferenceDoesNotBreak() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "/** {@link #blah} */",
            "public class Test {",
            "  void blah() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocLinkSelect() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Map;",
            "/** {@link Map#get} */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void diagnosticPosition() {
    CompilationTestHelper.newInstance(RemoveUnusedImports.class, getClass())
        .addSourceLines(
            "Test.java",
            "package test;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "// BUG: Diagnostic contains:",
            "import java.util.LinkedList;",
            "public class Test {",
            "  List<String> xs = new ArrayList<>();",
            "}")
        .doTest();
  }

  @Test
  public void useInJavadocParameter() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.List;",
            "import java.util.Collection;",
            "/** {@link List#containsAll(Collection)}  */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void qualifiedJavadoc() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.Map.Entry;",
            "/** {@link java.util.List} {@link Map.Entry} */",
            "public class Test {}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Map;",
            "/** {@link java.util.List} {@link Map.Entry} */",
            "public class Test {}")
        .doTest();
  }

  @Test
  public void parameterErasure() {
    testHelper
        .addInputLines(
            "in/A.java",
            "import java.util.Collection;",
            "public class A<T extends Collection> {",
            "  public void foo(T t) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/B.java",
            "import java.util.Collection;",
            "import java.util.List;",
            "public class B extends A<List> {",
            "  /** {@link #foo(Collection)} {@link #foo(List)} */",
            "  public void foo(List t) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void atSee() {
    testHelper
        .addInputLines(
            "Lib.java",
            "import java.nio.file.Path;",
            "class Lib {",
            "  static void f(Path... ps) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  /** @see Lib#f(Path[]) */",
            "  void f() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void multipleTopLevelClasses() {
    CompilationTestHelper.newInstance(RemoveUnusedImports.class, getClass())
        .addSourceLines(
            "MultipleTopLevelClasses.java",
            "import java.util.List;",
            "import java.util.Set;",
            "public class MultipleTopLevelClasses { List x; }",
            "class Evil { Set x; }")
        .doTest();
  }

  @Test
  public void unusedInPackageInfo() {
    testHelper
        .addInputLines(
            "in/com/example/package-info.java", "package com.example;", "import java.util.Map;")
        .addOutputLines(
            "out/com/example/package-info.java",
            "package com.example;",
            "") // The package statement's trailing newline is retained
        .doTest(TEXT_MATCH);
  }

  @Test
  public void b69984547() {
    testHelper
        .addInputLines(
            "android/app/PendingIntent.java",
            "package android.app;",
            "public class PendingIntent {",
            "}")
        .expectUnchanged()
        .addInputLines(
            "android/app/AlarmManager.java",
            "package android.app;",
            "public class AlarmManager {",
            "  public void set(int type, long triggerAtMillis, PendingIntent operation) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            "import android.app.PendingIntent;",
            "/** {@link android.app.AlarmManager#containsAll(int, long, PendingIntent)}  */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void b70690930() {
    testHelper
        .addInputLines(
            "a/One.java", //
            "package a;",
            "public class One {}")
        .expectUnchanged()
        .addInputLines(
            "a/Two.java", //
            "package a;",
            "public class Two {}")
        .expectUnchanged()
        .addInputLines(
            "p/Lib.java",
            "package p;",
            "import a.One;",
            "import a.Two;",
            "public class Lib {",
            "  private static class I {",
            "    public void f(One a) {}",
            "  }",
            "  public static class J {",
            "    public void f(Two a) {}",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "p/Test.java", //
            "package p;",
            "import a.One;",
            "import a.Two;",
            "/** {@link Lib.I#f(One)} {@link Lib.J#f(Two)} */",
            "public class Test {",
            "}")
        .addOutputLines(
            "out/p/Test.java", //
            "package p;",
            "import a.Two;",
            "/** {@link Lib.I#f(One)} {@link Lib.J#f(Two)} */",
            "public class Test {",
            "}")
        .doTest();
  }
}
