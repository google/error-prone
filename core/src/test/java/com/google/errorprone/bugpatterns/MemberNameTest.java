/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MemberName}. */
@RunWith(JUnit4.class)
public class MemberNameTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MemberName.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(MemberName.class, getClass());

  @Test
  public void nameWithUnderscores() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int foo_bar;",
            "  int get() {",
            "    return foo_bar;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int fooBar;",
            "  int get() {",
            "    return fooBar;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void localVariable_renamed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int get() {",
            "    int foo_bar = 1;",
            "    return foo_bar;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int get() {",
            "    int fooBar = 1;",
            "    return fooBar;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nameWithUnderscores_public_notRenamed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public int foo_bar;",
            "  int get() {",
            "    return foo_bar;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nameWithLeadingUppercase() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private int Foo;",
            "  int get() {",
            "    return Foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void upperCamelCaseAndNotStatic_noFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private int FOO;",
            "}")
        .doTest();
  }

  @Test
  public void upperCamelCaseAndStatic_noFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private static final int FOO_BAR = 1;",
            "}")
        .doTest();
  }

  @Test
  public void methodNamedParametersFor_noFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  public void parametersForMyFavouriteTest_whichHasUnderscores() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodAnnotatedWithAnnotationContainingTest_exempted() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  @IAmATest",
            "  public void possibly_a_test_name() {}",
            "  private @interface IAmATest {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreTestMissingTestAnnotation_exempted() {
    helper
        .addSourceLines(
            "Test.java", //
            "import org.junit.Ignore;",
            "class Test {",
            "  @Ignore",
            "  public void possibly_a_test_name() {}",
            "}")
        .doTest();
  }

  @Test
  public void superMethodAnnotatedWithAnnotationContainingTest_exempted() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  @IAmATest",
            "  public void possibly_a_test_name() {}",
            "  private @interface IAmATest {}",
            "}")
        .addSourceLines(
            "Test2.java",
            "class Test2 extends Test {",
            "  @Override",
            "  public void possibly_a_test_name() {}",
            "}")
        .doTest();
  }

  @Test
  public void nativeMethod_ignored() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  public native void possibly_a_test_name();",
            "}")
        .doTest();
  }

  @Test
  public void methodAnnotatedWithExemptedMethod_noMatch() {
    helper
        .addSourceLines(
            "Property.java", //
            "package com.pholser.junit.quickcheck;",
            "public @interface Property {}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  @com.pholser.junit.quickcheck.Property",
            "  public void possibly_a_test_name() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodWithUnderscores_overriddenFromGeneratedSupertype_noFinding() {
    helper
        .addSourceLines(
            "Base.java",
            "import javax.annotation.Generated;",
            "@Generated(\"Hands\")",
            "abstract class Base {",
            "  @SuppressWarnings(\"MemberName\")", // We only care about the subtype in this test.
            "  abstract int get_some();",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test extends Base {",
            "  @Override",
            "  public int get_some() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodWithUnderscores_notOverriddenFromGeneratedSupertype_bug() {
    helper
        .addSourceLines(
            "Base.java",
            "import javax.annotation.Generated;",
            "@Generated(\"Hands\")",
            "abstract class Base {}")
        .addSourceLines(
            "Test.java",
            "class Test extends Base {",
            "  // BUG: Diagnostic contains:",
            "  public int get_more() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }
}
