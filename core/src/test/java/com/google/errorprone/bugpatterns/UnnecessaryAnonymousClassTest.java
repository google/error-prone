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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnnecessaryAnonymousClass}Test */
@RunWith(JUnit4.class)
public class UnnecessaryAnonymousClassTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessaryAnonymousClass(), getClass());

  @Test
  public void variable_instance() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private final Function<String, String> camelCase = new Function<String, String>() {",
            "    public String apply(String x) {",
            "      return \"hello \" + x;",
            "    }",
            "  };",
            "  void g() {",
            "    Function<String, String> f = camelCase;",
            "    System.err.println(camelCase.apply(\"world\"));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private String camelCase(String x) {",
            "    return \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> f = this::camelCase;",
            "    System.err.println(camelCase(\"world\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variable_static() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static final Function<String, String> F = new Function<String, String>() {",
            "    public String apply(String x) {",
            "      return \"hello \" + x;",
            "    }",
            "  };",
            "  void g() {",
            "    Function<String, String> l = Test.F;",
            "    System.err.println(F.apply(\"world\"));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static String f(String x) {",
            "    return \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> l = Test::f;",
            "    System.err.println(f(\"world\"));",
            "  }",
            "}")
        .doTest();
  }
}
