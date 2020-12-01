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

/**
 * Unit tests for {@link ConstantPatternCompile} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ConstantPatternCompileTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ConstantPatternCompile.class, getClass());
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ConstantPatternCompile(), getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String MY_COOL_PATTERN = \"a+\";",
            "  public static void myPopularStaticMethod() {",
            "    // BUG: Diagnostic contains: ConstantPatternCompile",
            "    Pattern pattern = Pattern.compile(MY_COOL_PATTERN);",
            "    Pattern pattern2;",
            "    pattern2 = Pattern.compile(MY_COOL_PATTERN);",
            "  }",
            "  private void myPopularNonStaticMethod() {",
            "    // BUG: Diagnostic contains: ConstantPatternCompile",
            "    Pattern pattern = Pattern.compile(MY_COOL_PATTERN);",
            "  }",
            "  private void patternCalledOnLiteral() {",
            "    // BUG: Diagnostic contains: ConstantPatternCompile",
            "    Pattern pattern = Pattern.compile(\"literal\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFixGenerationStatic() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String MY_COOL_PATTERN = \"a+\";",
            "  public static void myPopularStaticMethod() {",
            "    Pattern somePattern = Pattern.compile(MY_COOL_PATTERN);",
            "    Matcher m = somePattern.matcher(\"aaaaab\");",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String MY_COOL_PATTERN = \"a+\";",
            "  public static void myPopularStaticMethod() {",
            "    Matcher m = SOME_PATTERN.matcher(\"aaaaab\");",
            "  }",
            "  private static final Pattern SOME_PATTERN = Pattern.compile(MY_COOL_PATTERN);",
            "}")
        .doTest();
  }

  @Test
  public void testFixGenerationWithJavadoc() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  /** This is a javadoc. **/ ",
            "  public static void myPopularStaticMethod() {",
            "    Pattern myPattern = Pattern.compile(\"a+\");",
            "    Matcher m = myPattern.matcher(\"aaaaab\");",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  /** This is a javadoc. **/ ",
            "  public static void myPopularStaticMethod() {",
            "    Matcher m = MY_PATTERN.matcher(\"aaaaab\");",
            "  }",
            "  private static final Pattern MY_PATTERN = Pattern.compile(\"a+\");",
            "}")
        .doTest();
  }

  @Test
  public void testFixGeneration_nonStaticInnerClass() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String MY_COOL_PATTERN = \"a+\";",
            "  class Inner {",
            "  public void myPopularStaticMethod() {",
            "      Pattern myPattern = Pattern.compile(MY_COOL_PATTERN);",
            "      Matcher m = myPattern.matcher(\"aaaaab\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String MY_COOL_PATTERN = \"a+\";",
            "  class Inner {",
            "  public void myPopularStaticMethod() {",
            "      Matcher m = MY_PATTERN.matcher(\"aaaaab\");",
            "    }",
            "  private final Pattern MY_PATTERN = Pattern.compile(MY_COOL_PATTERN);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static String pattern;",
            "  private static final Pattern MY_COOL_PATTERN = Pattern.compile(pattern);",
            "  private static final Pattern LOWER_CASE_ONLY = ",
            "    Pattern.compile(\"^([a-z]+)$\", Pattern.CASE_INSENSITIVE);",
            "  ",
            "  private void myPopularNonStaticMethod(String arg) {",
            "    Pattern pattern = Pattern.compile(arg + \"+\");",
            "  }",
            "  private void myPopularMethod(@CompileTimeConstant String arg) {",
            "    Pattern pattern = Pattern.compile(arg);",
            "  }",
            "  private final String patString = \"a+\";",
            "  private void patternCompileOnNonStaticArg() {",
            "    Pattern pattern = Pattern.compile(patString);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_multiArg() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  public static int getMatchCount(CharSequence content, String regex) {",
            "  int count = 0;",
            "  Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);",
            "  Matcher matcher = pattern.matcher(content);",
            "  while (matcher.find()) {",
            "    count++;",
            "  }",
            "  return count;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase_staticBlock() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String pattern = \"a+\";",
            "  static {",
            "    Pattern MY_COOL_PATTERN = Pattern.compile(pattern);",
            "  }",
            "}")
        .doTest();
  }
}
