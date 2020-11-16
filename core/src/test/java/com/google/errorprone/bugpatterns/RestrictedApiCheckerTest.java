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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.sun.tools.javac.main.Main.Result;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit test for {@link RestrictedApiChecker} */
@RunWith(JUnit4.class)
public class RestrictedApiCheckerTest {
  private final CompilationTestHelper helper;
  private final BugCheckerRefactoringTestHelper refactoringTest;

  public RestrictedApiCheckerTest() {
    this(RestrictedApiChecker.class);
  }

  protected RestrictedApiCheckerTest(Class<? extends BugChecker> checker) {
    helper =
        CompilationTestHelper.newInstance(checker, RestrictedApiCheckerTest.class)
            .addSourceFile("RestrictedApiMethods.java")
            .matchAllDiagnostics();
    refactoringTest =
        BugCheckerRefactoringTestHelper.newInstance(checker, RestrictedApiCheckerTest.class);
  }

  @Test
  public void testNormalCallAllowed() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo(RestrictedApiMethods m) {",
            "    m.normalMethod();",
            "    m.accept(m::normalMethod);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testRestrictedCallProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo(RestrictedApiMethods m) {",
            "    // BUG: Diagnostic contains: lorem",
            "    m.restrictedMethod();",
            "    // BUG: Diagnostic contains: lorem",
            "    m.accept(m::restrictedMethod);",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testRestrictedCallProhibited_inherited() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo(RestrictedApiMethods.Subclass m) {",
            "    // BUG: Diagnostic contains: lorem",
            "    m.restrictedMethod();",
            "    // BUG: Diagnostic contains: ipsum",
            "    m.dontCallMe();",
            "    // BUG: Diagnostic contains: lorem",
            "    m.accept(m::restrictedMethod);",
            "    // BUG: Diagnostic contains: ipsum",
            "    m.accept(m::dontCallMe);",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testRestrictedCallAllowedOnWhitelistedPath() {
    helper
        .addSourceLines(
            "testsuite/Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo(RestrictedApiMethods m) {",
            "    m.restrictedMethod();",
            "    m.accept(m::restrictedMethod);",
            "  }",
            "}")
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void testRestrictedStaticCallProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: lorem",
            "    RestrictedApiMethods.restrictedStaticMethod();",
            "    // BUG: Diagnostic contains: lorem",
            "    RestrictedApiMethods.accept(RestrictedApiMethods::restrictedStaticMethod);",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testRestrictedConstructorProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: lorem",
            "    new RestrictedApiMethods(0);",
            "    // BUG: Diagnostic contains: lorem",
            "    RestrictedApiMethods.accept(RestrictedApiMethods::new);",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testRestrictedConstructorViaAnonymousClassProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: lorem",
            "    new RestrictedApiMethods() {};",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testRestrictedConstructorViaAnonymousClassAllowed() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  @Allowlist    ",
            "  void foo() {",
            "    new RestrictedApiMethods() {};",
            "  }",
            "}")
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void testRestrictedCallAnonymousClassFromInterface() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  void foo() {",
            "    new IFaceWithRestriction() {",
            "      @Override",
            "      public void dontCallMe() {}",
            "    }",
            "    // BUG: Diagnostic contains: ipsum",
            "    .dontCallMe();",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testImplicitRestrictedConstructorProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase extends RestrictedApiMethods {",
            "  // BUG: Diagnostic contains: lorem",
            "  public Testcase() {}",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Ignore("Doesn't work yet")
  @Test
  public void testImplicitRestrictedConstructorProhibited_implicitConstructor() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "  // BUG: Diagnostic contains: lorem",
            "class Testcase extends RestrictedApiMethods {}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void testAllowWithWarning() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  @AllowlistWithWarning",
            "  void foo(RestrictedApiMethods m) {",
            "    // BUG: Diagnostic contains: lorem",
            "    m.restrictedMethod();",
            "    // BUG: Diagnostic contains: lorem",
            "    m.accept(m::restrictedMethod);",
            "  }",
            "}")
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void testAllowWithoutWarning() {
    helper
        .addSourceLines(
            "Testcase.java",
            "package com.google.errorprone.bugpatterns.testdata;",
            "class Testcase {",
            "  @Allowlist",
            "  void foo(RestrictedApiMethods m) {",
            "    m.restrictedMethod();",
            "    m.accept(m::restrictedMethod);",
            "  }",
            "}")
        .expectResult(Result.OK)
        .doTest();
  }

  // Regression test for b/36160747
  @Test
  public void testAllowAllDefinitionsInFile() {
    helper
        .addSourceLines(
            "Testcase.java",
            "",
            "package separate.test;",
            "",
            "import com.google.errorprone.annotations.RestrictedApi;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "",
            "class Testcase {",
            "   @Allowlist",
            "   void caller() {",
            "     restrictedMethod();",
            "   }",
            "   @RestrictedApi(",
            "     explanation=\"test\",",
            "     whitelistAnnotations = {Allowlist.class},",
            "     link = \"foo\"",
            "   )",
            "   void restrictedMethod() {",
            "   }",
            "   @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})",
            "   @interface Allowlist {}",
            "}")
        .doTest();
  }
}
