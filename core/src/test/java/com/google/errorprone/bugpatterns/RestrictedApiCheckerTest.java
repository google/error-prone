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

import com.google.errorprone.CompilationTestHelper;
import com.sun.tools.javac.main.Main.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit test for {@link RestrictedApiChecker} */
@RunWith(JUnit4.class)
public class RestrictedApiCheckerTest {
  private CompilationTestHelper helper;

  @Before
  public void setUp() {
    helper = CompilationTestHelper.newInstance(RestrictedApiChecker.class, getClass());
    helper.addSourceFile("RestrictedApiMethods.java");
    helper.matchAllDiagnostics();
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
            "  }",
            "}")
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
            "  @WhitelistWithWarning",
            "  void foo(RestrictedApiMethods m) {",
            "    // BUG: Diagnostic contains: [RestrictedApi]",
            "    m.restrictedMethod();",
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
            "  @Whitelist",
            "  void foo(RestrictedApiMethods m) {",
            "    m.restrictedMethod();",
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
            "   @Whitelist",
            "   void caller() {",
            "     restrictedMethod();",
            "   }",
            "   @RestrictedApi(",
            "     explanation=\"test\",",
            "     whitelistAnnotations = {Whitelist.class},",
            "     link = \"foo\"",
            "   )",
            "   void restrictedMethod() {",
            "   }",
            "   @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})",
            "   @interface Whitelist {}",
            "}")
        .doTest();
  }

}
