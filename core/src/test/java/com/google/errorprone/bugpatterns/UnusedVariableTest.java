/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.ErrorProneFlags;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnusedVariable}. */
@RunWith(JUnit4.class)
public class UnusedVariableTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnusedVariable.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          new UnusedVariable(ErrorProneFlags.empty()), getClass());

  @Test
  public void exemptedByReceiverParameter() {
    helper
        .addSourceLines(
            "ExemptedByReceiverParameter.java",
            "package unusedvars;",
            "public class ExemptedByReceiverParameter {",
            "  public void test() {",
            "    used();",
            "  }",
            "  private void used(ExemptedByReceiverParameter this) {",
            "    // the receiver parameter should not be marked as unused",
            "  }",
            "  class Inner {",
            "    private Inner(ExemptedByReceiverParameter ExemptedByReceiverParameter.this) {",
            "      // the receiver parameter should not be marked as unused",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unicodeBytes() {
    helper
        .addSourceLines(
            "UnicodeBytes.java",
            "package unusedvars;",
            "/**",
            " * This file contains Unicode characters: ❁❁❁❁❁❁❁❁❁",
            " */",
            "public class UnicodeBytes {",
            "  public void test() {",
            "    // BUG: Diagnostic contains: is never read",
            "    int notUsedLocal;",
            "    String usedLocal = \"\";",
            "    System.out.println(usedLocal);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedArray() {
    helper
        .addSourceLines(
            "UnusedArray.java",
            "package unusedvars;",
            "public class UnusedArray {",
            "  private int[] ints;",
            "  public void test() {",
            "    ints[0] = 0;",
            "    ints[0]++;",
            "    ints[0]--;",
            "    ints[0] -= 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedEnhancedForLoop() {
    refactoringHelper
        .addInputLines(
            "UnusedEnhancedForLoop.java",
            "package unusedvars;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class UnusedEnhancedForLoop {",
            "  public List<String> makeList(List<String> input) {",
            "    List<String> output = new ArrayList<>();",
            "    for (final String firstVar : input) {",
            "      output.add(\"a string\");",
            "    }",
            "    return output;",
            "  }",
            "  public List<String> listData(List<List<String>> input) {",
            "    List<String> output = new ArrayList<>();",
            "    for (List<String> secondVar : input) {",
            "      output.add(\"a string\");",
            "    }",
            "    return output;",
            "  }",
            "}")
        .addOutputLines(
            "UnusedEnhancedForLoop.java",
            "package unusedvars;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class UnusedEnhancedForLoop {",
            "  public List<String> makeList(List<String> input) {",
            "    List<String> output = new ArrayList<>();",
            "    for (final String unused : input) {",
            "      output.add(\"a string\");",
            "    }",
            "    return output;",
            "  }",
            "  public List<String> listData(List<List<String>> input) {",
            "    List<String> output = new ArrayList<>();",
            "    for (List<String> unused : input) {",
            "      output.add(\"a string\");",
            "    }",
            "    return output;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedField() {
    helper
        .addSourceLines(
            "UnusedField.java",
            "package unusedvars;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public class UnusedField {",
            "  // BUG: Diagnostic contains: is never read",
            "  private int notUsedInt;",
            "  // BUG: Diagnostic contains: is never read",
            "  private List<String> list = new ArrayList<>();",
            "  public void test() {",
            "    notUsedInt = 0;",
            "    if (hashCode() > 0) {",
            "      list = null;",
            "    } else {",
            "      list = makeList();",
            "    }",
            "  }",
            "  private List<String> makeList() {",
            "    return null;",
            "  }",
            "  public UnusedField(List<String> list) {",
            "    this.list = list;",
            "  }",
            "  // These fields are special, and should not be flagged as unused.",
            "  private static final long serialVersionUID = 0;",
            "  private static final String TAG = \"UnusedFieldTestThingy\";",
            "  @SuppressWarnings(\"unchecked\")",
            "  // BUG: Diagnostic contains: is never read",
            "  private long fieldWithAnn;",
            "}")
        .doTest();
  }

  @Test
  public void unusedLocalVarInitialized() {
    helper
        .addSourceLines(
            "UnusedLocalVarInitialized.java",
            "package unusedvars;",
            "public class UnusedLocalVarInitialized {",
            "  public void test() {",
            "    String s = \"\";",
            "    System.out.println(s);",
            "    // BUG: Diagnostic contains: is never read",
            "    int notUsed = UnusedLocalVarInitialized.setData();",
            "    notUsed = this.hashCode();",
            "  }",
            "  public static int setData() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedLocalVar() {
    helper
        .addSourceLines(
            "UnusedLocalVar.java",
            "package unusedvars;",
            "public class UnusedLocalVar {",
            "  public void test() {",
            "    // BUG: Diagnostic contains: is never read",
            "    int notUsedLocal;",
            "    notUsedLocal = 0;",
            "    String usedLocal = \"\";",
            "    if (usedLocal.length() == 0) {",
            "      notUsedLocal = 10 + usedLocal.length();",
            "    } else {",
            "      notUsedLocal = this.calculate()",
            "          + 1  ;",
            "      notUsedLocal--;",
            "      notUsedLocal += Integer.valueOf(1);",
            "      System.out.println(usedLocal);",
            "    }",
            "    System.out.println(usedLocal);",
            "  }",
            "  int calculate() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedNative() {
    helper
        .addSourceLines(
            "UnusedNative.java",
            "package unusedvars;",
            "public class UnusedNative {",
            "  private int usedInNative1 = 0;",
            "  private String usedInNative2 = \"\";",
            "  private native void aNativeMethod();",
            "}")
        .doTest();
  }

  @Test
  public void unusedParamInPrivateMethod() {
    helper
        .addSourceLines(
            "UnusedParamInPrivateMethod.java",
            "package unusedvars;",
            "public class UnusedParamInPrivateMethod {",
            "  // BUG: Diagnostic contains: 'j' is never read",
            "  private void test(int i, int j) {",
            "    System.out.println(i);",
            "  }",
            "  public void main() {",
            "    test(1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unuseds() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "import java.io.IOException;",
            "import java.io.ObjectStreamException;",
            "import java.util.List;",
            "import javax.inject.Inject;",
            "public class Unuseds {",
            "  // BUG: Diagnostic contains:",
            "  private static final String NOT_USED_CONST_STR = \"unused_test\";",
            "  private static final String CONST_STR = \"test\";",
            "  // BUG: Diagnostic contains:",
            "  private int notUsed;",
            "  private List<String> used;",
            "  public int publicOne;",
            "  private int[] ints;",
            "  @Inject",
            "  private int unusedExemptedByAnnotation;",
            "  @Inject",
            "  private void unusedMethodExemptedByAnnotation() {}",
            "  void test() {",
            "    this.notUsed = 0;",
            "    this.notUsed++;",
            "    this.notUsed += 0;",
            "    // BUG: Diagnostic contains:",
            "    int notUsedLocal;",
            "    notUsedLocal = 10;",
            "    int usedLocal = 0;",
            "    if (!used.get(usedLocal).toString().equals(CONST_STR)) {",
            "      used.add(\"test\");",
            "    }",
            "    // j is used",
            "    int j = 0;",
            "    used.get(j++);",
            "    ints[j--]++;",
            "    ints[1] = 0;",
            "    // Negative case (:( data flow analysis...).",
            "    byte[] notUsedLocalArray = new byte[]{};",
            "    notUsedLocalArray[0] += this.used.size();",
            "    char[] out = new char[]{};",
            "    for (int m = 0, n = 0; m < 1; m++) {",
            "      out[n++] = out[m];",
            "    }",
            "    // Negative case",
            "    double timestamp = 0.0;",
            "    set(timestamp += 1.0);",
            "    int valuesIndex1 = 0;",
            "    int valuesIndex2 = 0;",
            "    double[][][] values = null;",
            "    values[0][valuesIndex1][valuesIndex2] = 10;",
            "    System.out.println(values);",
            "  }",
            "  public void set(double d) {}",
            "  public void usedInMethodCall(double d) {",
            "    List<Unuseds> notUseds = null;",
            "    int indexInMethodCall = 0;",
            "    // Must not be reported as unused",
            "    notUseds.get(indexInMethodCall).publicOne = 0;",
            "  }",
            "  void memberSelectUpdate1() {",
            "    List<Unuseds> l = null;",
            "    // `u` should not be reported as unused.",
            "    Unuseds u = getFirst(l);",
            "    u.notUsed = 10;",
            "    System.out.println(l);",
            "    getFirst(l).notUsed = 100;",
            "  }",
            "  void memberSelectUpdate2() {",
            "    List<Unuseds> l = null;",
            "    // `l` should not be reported as unused.",
            "    l.get(0).notUsed = 10;",
            "  }",
            "  Unuseds getFirst(List<Unuseds> l) {",
            "    return l.get(0);",
            "  }",
            "  // Negative case. Must not report.",
            "  private int usedCount = 0;",
            "  int incCounter() {",
            "    return usedCount += 2;",
            "  }",
            "  // For testing the lack of NPE on return statement.",
            "  public void returnNothing() {",
            "    return;",
            "  }",
            "  // Negative case. Must not report.",
            "  public void testUsedArray() {",
            "    ints[0] = 0;",
            "    ints[0]++;",
            "    ints[0]--;",
            "    ints[0] -= 0;",
            "  }",
            "  @SuppressWarnings({\"deprecation\", \"unused\"})",
            "  class UsesSuppressWarning {",
            "    private int f1;",
            "    private void test1() {",
            "      int local;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Unuseds.java",
            "package unusedvars;",
            "public class Unuseds {",
            "  private static final String NOT_USED_CONST_STR = \"unused_test\";",
            "  private static final String CONST_STR = \"test\";",
            "  private int notUsed;",
            "  public int publicOne;",
            "  void test() {",
            "    this.notUsed = 0;",
            "    this.notUsed++;",
            "    this.notUsed += 0;",
            "    int notUsedLocal;",
            "    notUsedLocal = 10;",
            "    System.out.println(CONST_STR);",
            "  }",
            "}")
        .addOutputLines(
            "Unuseds.java",
            "package unusedvars;",
            "public class Unuseds {",
            "  private static final String CONST_STR = \"test\";",
            "  public int publicOne;",
            "  void test() {",
            "    System.out.println(CONST_STR);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overridableMethods() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "public class Unuseds {",
            "  // BUG: Diagnostic contains: The parameter 'j' is never read",
            "  private int usedPrivateMethodWithUnusedParam(int i, int j) {",
            "    return i * 2;",
            "  }",
            "  int a = usedPrivateMethodWithUnusedParam(1, 2);",
            "  // In the following three cases, parameters should not be reported as unused,",
            "  // because methods are non-private.",
            "  public void publicMethodWithUnusedParam(int i, int j) {}",
            "  public void protectedMethodWithUnusedParam(int i, int j) {}",
            "  public void packageMethodWithUnusedParam(int i, int j) {}",
            "}")
        .doTest();
  }

  @Test
  public void exemptedByName() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "class ExemptedByName {",
            "  private int unused;",
            "  private int unusedInt;",
            "  private static final int UNUSED_CONSTANT = 5;",
            "  private int ignored;",
            "}")
        .doTest();
  }

  @Test
  public void suppressions() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "class Suppressed {",
            "  @SuppressWarnings({\"deprecation\", \"unused\"})",
            "  class UsesSuppressWarning {",
            "    private int f1;",
            "    private void test1() {",
            "      int local;",
            "    }",
            "    @SuppressWarnings(value = \"unused\")",
            "    private void test2() {",
            "      int local;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedStaticField() {
    helper
        .addSourceLines(
            "UnusedStaticField.java",
            "package unusedvars;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public class UnusedStaticField {",
            "  // BUG: Diagnostic contains: is never read",
            "  private static final List<String> DATA = new ArrayList<>();",
            "}")
        .doTest();
  }

  @Test
  public void unusedStaticPrivate() {
    helper
        .addSourceLines(
            "UnusedStaticPrivate.java",
            "package unusedvars;",
            "public class UnusedStaticPrivate {",
            "  // BUG: Diagnostic contains: is never read",
            "  private static final String NOT_USED_CONST_STR = \"unused_test\";",
            "  static final String CONST_STR = \"test\";",
            "}")
        .doTest();
  }

  @Test
  public void unusedTryResource() {
    helper
        .addSourceLines(
            "UnusedTryResource.java",
            "package unusedvars;",
            "public class UnusedTryResource {",
            "  public static void main(String[] args) {",
            "    try (A a = new A()) {",
            "    }",
            "  }",
            "}",
            "class A implements AutoCloseable {",
            "  public void close() {}",
            "}")
        .doTest();
  }

  @Test
  public void removal_javadocsAndNonJavadocs() {
    refactoringHelper
        .addInputLines(
            "UnusedWithComment.java",
            "package unusedvars;",
            "public class UnusedWithComment {",
            "  /**",
            "   * Comment for a field */",
            "  @SuppressWarnings(\"test\")",
            "  private Object field;",
            "}")
        .addOutputLines(
            "UnusedWithComment.java", //
            "package unusedvars;",
            "public class UnusedWithComment {",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removal_trailingComment() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "public class Test {",
            "  public static final int A = 1; // foo",
            "",
            "  private static final int B = 2;",
            "}")
        .addOutputLines(
            "Test.java", //
            "public class Test {",
            "  public static final int A = 1; // foo",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removal_javadocAndSingleLines() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "public class Test {",
            "  public static final int A = 1; // foo",
            "",
            "  /** Javadoc. */",
            "  // TODO: fix",
            "  // BUG: bug",
            "  private static final int B = 2;",
            "}")
        .addOutputLines(
            "Test.java", //
            "public class Test {",
            "  public static final int A = 1; // foo",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removal_rogueBraces() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "@SuppressWarnings(\"foo\" /* { */)",
            "public class Test {",
            "  private static final int A = 1;",
            "}")
        .addOutputLines(
            "Test.java", //
            "@SuppressWarnings(\"foo\" /* { */)",
            "public class Test {",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void unusedWithComment_interspersedComments() {
    helper
        .addSourceLines(
            "UnusedWithComment.java",
            "package unusedvars;",
            "public class UnusedWithComment {",
            "  private static final String foo = null, // foo",
            "  // BUG: Diagnostic contains:",
            "      bar = null;",
            "  public static String foo() { return foo; }",
            "}")
        .doTest();
  }

  @Test
  public void usedInLambda() {
    helper
        .addSourceLines(
            "UsedInLambda.java",
            "package unusedvars;",
            "import java.util.Arrays;",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import java.util.stream.Collectors;",
            "/** Method parameters used in lambdas and anonymous classes */",
            "public class UsedInLambda {",
            "  private Function<Integer, Integer> usedInLambda() {",
            "    return x -> 1;",
            "  }",
            "  private String print(Object o) {",
            "    return o.toString();",
            "  }",
            "  public List<String> print(List<Object> os) {",
            "    return os.stream().map(this::print).collect(Collectors.toList());",
            "  }",
            "  public static void main(String[] args) {",
            "    System.err.println(new UsedInLambda().usedInLambda());",
            "    System.err.println(new UsedInLambda().print(Arrays.asList(1, 2, 3)));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void utf8Handling() {
    helper
        .addSourceLines(
            "Utf8Handling.java",
            "package unusedvars;",
            "public class Utf8Handling {",
            "  private int foo = 1;",
            "  public void test() {",
            "    System.out.println(\"広\");",
            "    for (int i = 0; i < 10; ++i) {",
            "    // BUG: Diagnostic contains: is never read",
            "      int notUsedLocal = calculate();",
            "    }",
            "    System.out.println(foo);",
            "  }",
            "  int calculate() {",
            "    return ++foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodAnnotationsExemptingParameters() {
    helper
        .addSourceLines(
            "A.java",
            "package unusedvars;",
            "class A {",
            "  { foo(1); }",
            "  @B",
            "  private static void foo(int a) {}",
            "}",
            "@interface B {}")
        .setArgs(
            ImmutableList.of("-XepOpt:Unused:methodAnnotationsExemptingParameters=unusedvars.B"))
        .doTest();
  }

  @Test
  public void usedUnaryExpression() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "import java.util.Map;",
            "import java.util.HashMap;",
            "public class Test {",
            "  private int next = 1;",
            "  private Map<String, Integer> xs = new HashMap<>();",
            "  public int frobnicate(String s) {",
            "    Integer x = xs.get(s);",
            "    if (x == null) {",
            "      x = next++;",
            "      xs.put(s, x);",
            "    }",
            "    return x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedInject() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "import javax.inject.Inject;",
            "public class Test {",
            // Package-private @Inject fields are assumed to be only used within the class, and only
            // visible for performance.
            "  // BUG: Diagnostic contains:",
            "  @Inject Object foo;",
            "  @Inject public Object bar;",
            "}")
        .setArgs(ImmutableList.of("-XepOpt:Unused:ReportInjectedFields"))
        .doTest();
  }

  @Test
  public void unusedInject_notByDefault() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "import javax.inject.Inject;",
            "public class Test {",
            "  @Inject Object foo;",
            "  @Inject public Object bar;",
            "}")
        .doTest();
  }

  @Test
  public void variableKeepingSideEffects() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  private final ImmutableList<Integer> foo = ImmutableList.of();",
            "  void test() {",
            "     ImmutableList<Integer> foo = ImmutableList.of();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  { ImmutableList.of(); }",
            "  void test() {",
            "     ImmutableList.of();",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void variableRemovingSideEffects() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  private final ImmutableList<Integer> foo = ImmutableList.of();",
            "  void test() {",
            "     ImmutableList<Integer> foo = ImmutableList.of();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  void test() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void exemptedFieldsByType() {
    helper
        .addSourceLines(
            "Test.java",
            "import org.junit.rules.TestRule;",
            "class Test {",
            "  private TestRule rule;",
            "}")
        .doTest();
  }

  @Test
  public void findingBaseSymbol() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int a;",
            "  void test() {",
            "    Test o = new Test();",
            "    ((Test) o).a = 1;",
            "    (((o))).a = 1;",
            "    Test p = new Test();",
            "    id(p).a = 1;",
            "  }",
            "  Test id(Test t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void fixPrivateMethod_usagesToo() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1);",
            "  private int foo(int b) {",
            "    b = 1;",
            "    return 1;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int a = foo();",
            "  private int foo() {",
            "    return 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fixPrivateMethod_parameterLocations() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1, 2, 3) + bar(1, 2, 3) + baz(1, 2, 3);",
            "  private int foo(int a, int b, int c) { return a * b; }",
            "  private int bar(int a, int b, int c) { return b * c; }",
            "  private int baz(int a, int b, int c) { return a * c; }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1, 2) + bar(2, 3) + baz(1, 3);",
            "  private int foo(int a, int b) { return a * b; }",
            "  private int bar(int b, int c) { return b * c; }",
            "  private int baz(int a, int c) { return a * c; }",
            "}")
        .doTest();
  }

  @Test
  public void fixPrivateMethod_varArgs() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1, 2, 3, 4);",
            "  private int foo(int a, int... b) { return a; }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1);",
            "  private int foo(int a) { return a; }",
            "}")
        .doTest();
  }

  @Test
  public void fixPrivateMethod_varArgs_noArgs() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1);",
            "  private int foo(int a, int... b) { return a; }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int a = foo(1);",
            "  private int foo(int a) { return a; }",
            "}")
        .doTest();
  }

  @Ignore("b/118437729")
  @Test
  public void enumField() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "enum Test {",
            "  ONE(\"1\", 1) {};",
            "  private String a;",
            "  private Test(String a, int x) {",
            "    this.a = a;",
            "  }",
            "  String a() {",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "enum Test {",
            "  ONE(\"1\") {};",
            "  private String a;",
            "  private Test(String a) {",
            "    this.a = a;",
            "  }",
            "  String a() {",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("b/118437729")
  @Test
  public void onlyEnumField() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "enum Test {",
            "  ONE(1) {};",
            "  private Test(int x) {",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "enum Test {",
            "  ONE() {};",
            "  private Test() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sideEffectFix() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private static final int[] xs = new int[0];",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "}")
        .doTest();
  }

  @Test
  public void sideEffectFieldFix() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private int x = 1;",
            "  public int a() {",
            "    x = a();",
            "    return 1;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  public int a() {",
            "    a();",
            "    return 1;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void blockFixTest() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void foo() {",
            "    int a = 1;",
            "    if (hashCode() > 0)",
            "      a = 2;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void foo() {",
            "    if (hashCode() > 0) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignment() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    Integer a = 1;",
            "    a.hashCode();",
            "    // BUG: Diagnostic contains: assignment to",
            "    a = 2;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignment_messages() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public int test() {",
            "    // BUG: Diagnostic contains: This assignment to the local variable",
            "    int a = 1;",
            "    a = 2;",
            "    int b = a;",
            "    int c = b;",
            "    // BUG: Diagnostic contains: This assignment to the local variable",
            "    b = 2;",
            "    b = 3;",
            "    return b + c;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignment_nulledOut_noWarning() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    Integer a = 1;",
            "    a.hashCode();",
            "    a = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignment_nulledOut_thenAssignedAgain() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    Integer a = 1;",
            "    a.hashCode();",
            "    a = null;",
            "    a = 2;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    Integer a = 1;",
            "    a.hashCode();",
            "    a = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignment_initialAssignmentNull_givesWarning() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    // BUG: Diagnostic contains: assignment",
            "    Integer a = null;",
            "    a = 1;",
            "    a.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignmentAfterUse() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    int a = 1;",
            "    System.out.println(a);",
            "    a = 2;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    int a = 1;",
            "    System.out.println(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignmentWithFinalUse() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    int a = 1;",
            "    a = 2;",
            "    a = 3;",
            "    System.out.println(a);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    int a = 3;",
            "    System.out.println(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentUsedInExpression() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public int test() {",
            "    int a = 1;",
            "    a = a * 2;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentToParameter() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test(int a) {",
            "    a = 2;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test(int a) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentToParameter_thenUsed() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public int test(int a) {",
            "    a = 2;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentToEnhancedForLoop() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test(Iterable<Integer> as) {",
            "    for (int a : as) {",
            "      System.out.println(a);",
            "      a = 2;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test(Iterable<Integer> as) {",
            "    for (int a : as) {",
            "      System.out.println(a);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentWithinForLoop() {
    helper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public void test() {",
            "    for (int a = 0; a < 10; a = a + 1) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentSeparateFromDeclaration_noComplaint() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public void test() {",
            "    int a;",
            "    a = 1;",
            "    System.out.println(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedAssignment_negatives() {
    helper
        .addSourceLines(
            "Test.java",
            "package unusedvars;",
            "public class Test {",
            "  public int frobnicate() {",
            "    int a = 1;",
            "    if (hashCode() == 0) {",
            "      a = 2;",
            "    }",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void exemptedMethods() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "import java.io.IOException;",
            "import java.io.ObjectStreamException;",
            "public class Unuseds implements java.io.Serializable {",
            "  private void readObject(java.io.ObjectInputStream in) throws IOException {}",
            "  private void writeObject(java.io.ObjectOutputStream out) throws IOException {}",
            "  private Object readResolve() {",
            "    return null;",
            "  }",
            "  private void readObjectNoData() throws ObjectStreamException {}",
            "}")
        .doTest();
  }
}
