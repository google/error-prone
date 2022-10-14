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
      BugCheckerRefactoringTestHelper.newInstance(ConstantPatternCompile.class, getClass());

  @Test
  public void testInlineExpressions() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isCar(String input) {",
            "    return Pattern.compile(\"car\").matcher(input).matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isCar(String input) {",
            "    return INPUT_PATTERN.matcher(input).matches();",
            "  }",
            "  private static final Pattern INPUT_PATTERN = Pattern.compile(\"car\");",
            "}")
        .doTest();
  }

  @Test
  public void testVariableNameFromField() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String INPUT = null;",
            "  boolean isCar() {",
            "    return Pattern.compile(\"car\").matcher(INPUT).matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final String INPUT = null;",
            "  boolean isCar() {",
            "    return INPUT_PATTERN.matcher(INPUT).matches();",
            "  }",
            "  private static final Pattern INPUT_PATTERN = Pattern.compile(\"car\");",
            "}")
        .doTest();
  }

  @Test
  public void testInlineExpression_argumentIsMethodCall() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  String getText() {return null;}",
            "  boolean isCar() {",
            "    return Pattern.compile(\"car\").matcher(getText()).matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  String getText() {return null;}",
            "  boolean isCar() {",
            "    return GET_TEXT_PATTERN.matcher(getText()).matches();",
            "  }",
            "  private static final Pattern GET_TEXT_PATTERN = Pattern.compile(\"car\");",
            "}")
        .doTest();
  }

  @Test
  public void testInlineExpression_nameDefaultsToPattern() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isCar() {",
            "    return Pattern.compile(\"car\").matcher(\"\").matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isCar() {",
            "    return PATTERN.matcher(\"\").matches();",
            "  }",
            "  private static final Pattern PATTERN = Pattern.compile(\"car\");",
            "}")
        .doTest();
  }

  @Test
  public void testMultipleInlineExpressions() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isBlueCar(String input) {",
            "    return Pattern.compile(\"car\").matcher(input).matches()",
            "      && Pattern.compile(\"blue\").matcher(input).matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isBlueCar(String input) {",
            "    return INPUT_PATTERN.matcher(input).matches()",
            "      && INPUT_PATTERN2.matcher(input).matches();",
            "  }",
            "  private static final Pattern INPUT_PATTERN = Pattern.compile(\"car\");",
            "  private static final Pattern INPUT_PATTERN2 = Pattern.compile(\"blue\");",
            "}")
        .doTest();
  }

  @Test
  public void testSameNameInDifferentMethods() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isCar(String input) {",
            "    return Pattern.compile(\"car\").matcher(input).matches();",
            "  }",
            "  boolean isDog(String input) {",
            "    return Pattern.compile(\"dog\").matcher(input).matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Matcher;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  boolean isCar(String input) {",
            "    return INPUT_PATTERN.matcher(input).matches();",
            "  }",
            "  boolean isDog(String input) {",
            "    return INPUT_PATTERN2.matcher(input).matches();",
            "  }",
            "  private static final Pattern INPUT_PATTERN = Pattern.compile(\"car\");",
            "  private static final Pattern INPUT_PATTERN2 = Pattern.compile(\"dog\");",
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
            "  static final String MY_COOL_PATTERN = \"a+\";",
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
            "  static final String MY_COOL_PATTERN = \"a+\";",
            "  public static void myPopularStaticMethod() {",
            "    Matcher m = SOME_PATTERN.matcher(\"aaaaab\");",
            "  }",
            "  private static final Pattern SOME_PATTERN = Pattern.compile(MY_COOL_PATTERN);",
            "}")
        .doTest();
  }

  @Test
  public void testFixGeneration_multiplePatterns() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  public static boolean match() {",
            "    String line = \"abcd\";",
            "    Pattern p1 = Pattern.compile(\"a+\");",
            "    Pattern p2 = Pattern.compile(\"b+\");",
            "    if (p1.matcher(line).matches() && p2.matcher(line).matches()) {",
            "      return true;",
            "    }",
            "    Pattern p3 = Pattern.compile(\"c+\");",
            "    Pattern p4 = Pattern.compile(\"d+\");",
            "    return p3.matcher(line).matches() && p4.matcher(line).matches();",
            "  }",
            "}")
        .addOutputLines(
            "in/Test.java",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  public static boolean match() {",
            "    String line = \"abcd\";",
            "    if (P1.matcher(line).matches() && P2.matcher(line).matches()) {",
            "      return true;",
            "    }",
            "    return P3.matcher(line).matches() && P4.matcher(line).matches();",
            "  }",
            "  private static final Pattern P1 = Pattern.compile(\"a+\");",
            "  private static final Pattern P2 = Pattern.compile(\"b+\");",
            "  private static final Pattern P3 = Pattern.compile(\"c+\");",
            "  private static final Pattern P4 = Pattern.compile(\"d+\");",
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
            "  static final String MY_COOL_PATTERN = \"a+\";",
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
            "  static final String MY_COOL_PATTERN = \"a+\";",
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

  // Don't convert String constants to patterns if they're used anywhere other than a single
  // Pattern.compile call.

  @Test
  public void testOnlyCode_noFinding() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  public static void test() {",
            "    Pattern pattern = Pattern.compile(\".*\");",
            "  }",
            "}")
        .setArgs("-XepCompilingTestOnlyCode")
        .doTest();
  }

  @Test
  public void withinList_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private static final ImmutableList<Pattern> patterns =",
            "      ImmutableList.of(Pattern.compile(\".*\"));",
            "}")
        .doTest();
  }
}
