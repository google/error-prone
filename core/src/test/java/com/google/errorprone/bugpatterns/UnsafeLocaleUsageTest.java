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

/** Tests for {@link UnsafeLocaleUsage}. */
@RunWith(JUnit4.class)
public class UnsafeLocaleUsageTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnsafeLocaleUsage.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnsafeLocaleUsage.class, getClass());

  @Test
  public void unsafeLocaleUsageCheck_constructorUsageWithOneParam_shouldRefactorNonLiteralParam() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a) {",
            "       locale = new Locale(a);",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh_hant_tw\");",
            "",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a) {",
            "       locale = Locale.forLanguageTag(a.replace(\"_\", \"-\"));",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh_hant_tw\");",
            "",
            "}")
        .doTest();
  }

  @Test
  public void unsafeLocaleUsageCheck_constructorUsageWithOneParam_shouldRefactorLiteralParam() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  private static final Locale LOCALE = new Locale(\"zh_hant_tw\");",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  private static final Locale LOCALE = Locale.forLanguageTag(\"zh-hant-tw\");",
            "}")
        .doTest();
  }

  @Test
  public void unsafeLocaleUsageCheck_constructorUsageWithTwoParams_shouldRefactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a, String b) {",
            "       locale = new Locale(a, b);",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh\", \"tw\");",
            "",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a, String b) {",
            "       locale = new Locale.Builder().setLanguage(a).setRegion(b).build();",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh\", \"tw\");",
            "",
            "}")
        .doTest();
  }

  @Test
  public void unsafeLocaleUsageCheck_constructorUsageWithThreeParams_shouldFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a, String b, String c) {",
            "       // BUG: Diagnostic contains: forLanguageTag(String)",
            "       locale = new Locale(a, b, c);",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh\", \"tw\", \"hant\");",
            "",
            "}")
        .doTest();
  }

  @Test
  public void unsafeLocaleUsageCheck_toStringUsage_shouldRefactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a) {",
            "       locale = Locale.forLanguageTag(a);",
            "    }",
            "",
            "    String getLocaleDisplayString() {",
            "       return locale.toString();",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh_hant_tw\");",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Locale;",
            "",
            "class Test {",
            "  static class Inner {",
            "    private Locale locale;",
            "    Inner(String a) {",
            "       locale = Locale.forLanguageTag(a);",
            "    }",
            "",
            "    String getLocaleDisplayString() {",
            "       return locale.toLanguageTag();",
            "    }",
            "  }",
            "",
            "  private static final Test.Inner INNER_OBJ = new Inner(\"zh_hant_tw\");",
            "}")
        .doTest();
  }

  @Test
  public void unsafeLocaleUsageCheck_multipleErrors_shouldFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Locale;",
            "class Test {",
            "    // BUG: Diagnostic contains: forLanguageTag(String)",
            "    private static final Locale LOCALE = new Locale(",
            "        // BUG: Diagnostic contains: toLanguageTag()",
            "        Locale.TAIWAN.toString());",
            "}")
        .doTest();
  }

  @Test
  public void unsafeLocaleUsageCheck_instanceMethodUsage_shouldNotFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Locale;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  private static final ImmutableMap<String, Locale> INTERNAL_COUNTRY_CODE_TO_LOCALE =",
            "    ImmutableMap.of(\"abc\", Locale.KOREAN);",
            "  private static final String DISPLAY_NAME = getLocaleDisplayNameFromCode(\"abc\");",
            "",
            "  public static final String getLocaleDisplayNameFromCode(String code) {",
            "    return INTERNAL_COUNTRY_CODE_TO_LOCALE.get(code).getDisplayName();",
            "  }",
            "}")
        .doTest();
  }
}
