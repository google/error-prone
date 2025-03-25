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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.dataflow.DataFlow.expressionDataflow;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author deminguyen@google.com (Demi Nguyen)
 */
@RunWith(JUnit4.class)
public class NullnessPropagationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NullnessPropagationChecker.class, getClass());

  /**
   * This method triggers the {@code BugPattern} used to test nullness propagation
   *
   * @param obj Variable whose nullness value is being checked
   * @return {@code obj} for use in expressions
   */
  public static Object triggerNullnessChecker(Object obj) {
    return obj;
  }

  /*
   * Methods that should never be called. These methods exist to force tests that pass a primitive
   * to decide whether they are testing (a) that primitive itself is null in which case they should
   * call triggerNullnessCheckerOnPrimitive or (b) that the result of autoboxing the primitive is
   * null, in which case it should call triggerNullnessCheckerOnBoxed. Of course, in either case,
   * the value should never be null. (The analysis isn't yet smart enough to detect this in all
   * cases.)
   *
   * Any call to these methods will produce a special error.
   */

  public static void triggerNullnessChecker(boolean b) {}

  public static void triggerNullnessChecker(byte b) {}

  public static void triggerNullnessChecker(char c) {}

  public static void triggerNullnessChecker(double d) {}

  public static void triggerNullnessChecker(float f) {}

  public static void triggerNullnessChecker(int i) {}

  public static void triggerNullnessChecker(long l) {}

  public static void triggerNullnessChecker(short s) {}

  /**
   * This method also triggers the {@code BugPattern} used to test nullness propagation, but it is
   * intended to be used only in the rare case of testing the result of boxing a primitive.
   */
  public static void triggerNullnessCheckerOnBoxed(Object obj) {}

  /*
   * These methods also trigger the {@code BugPattern} used to test nullness propagation, but they
   * are careful not to autobox their inputs.
   */

  public static void triggerNullnessCheckerOnPrimitive(boolean b) {}

  public static void triggerNullnessCheckerOnPrimitive(byte b) {}

  public static void triggerNullnessCheckerOnPrimitive(char c) {}

  public static void triggerNullnessCheckerOnPrimitive(double d) {}

  public static void triggerNullnessCheckerOnPrimitive(float f) {}

  public static void triggerNullnessCheckerOnPrimitive(int i) {}

  public static void triggerNullnessCheckerOnPrimitive(long l) {}

  public static void triggerNullnessCheckerOnPrimitive(short s) {}

  /** For {@link #testConstantsDefinedInOtherCompilationUnits}. */
  public static final String COMPILE_TIME_CONSTANT = "not null";

  /** For {@link #testConstantsDefinedInOtherCompilationUnits} as constant outside compilation. */
  @SuppressWarnings("GoodTime") // false positive
  public static final Integer NOT_COMPILE_TIME_CONSTANT = 421;

  @Test
  public void transferFunctions1() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases1.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation, primarily around
 * conditionals.
 */
public class NullnessPropagationTransferCases1 {

  public void conditionalNot(String foo) {
    if (!(foo == null)) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(foo);
      return;
    }

    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(foo);
  }

  public void conditionalOr1(String foo, String bar) {
    if (foo == null || bar == null) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(foo);
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(bar);
      return;
    }

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(foo);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(bar);
  }

  public void conditionalOr2(String foo, String bar) {
    if (foo != null || bar != null) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalOr3(String foo) {
    if (foo != null || foo != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalOr4(String foo) {
    // BUG: Diagnostic contains: (Non-null)
    if (foo == null || triggerNullnessChecker(foo) == null) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Null)
    if (foo != null || triggerNullnessChecker(foo) != null) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalAnd1(String foo, String bar) {
    if (foo != null && bar != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalAnd2(String foo) {
    if (foo == null && foo != null) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(foo);
      return;
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalAnd3(String foo) {
    // BUG: Diagnostic contains: (Null)
    if (foo == null && triggerNullnessChecker(foo) == null) {
      // Something
    }

    // BUG: Diagnostic contains: (Non-null)
    if (foo != null && triggerNullnessChecker(foo) != null) {
      // Something
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void ternary1(String nullable) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(nullable == null ? "" : nullable);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(nullable != null ? null : nullable);
  }

  public void ternary2(boolean test, String nullable) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(test ? "yes" : "no");
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(test ? nullable : "");
  }

  public void valueOfComparisonItself() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 == 1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 != 1);
    boolean b;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b = (1 == 1));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b = (1 != 1));

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(!b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b || b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b && b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b = !b);
  }

  public void leastUpperBoundOfNonNullAndUnknown(String param, boolean b) {
    if (b) {
      param = "foo";
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(param);
  }

  public void stringConcatenation(String a, String b) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a + b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(null + b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a + 5);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(null + (String) null);
  }
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions2() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases2.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;
import static com.google.errorprone.dataflow.nullnesspropagation.testdata.NullnessPropagationTransferCases2.MyEnum.ENUM_INSTANCE;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation around constants
 * and built-in knowledge.
 */
@SuppressWarnings("deprecation") // test cases include deprecated JUnit methods
public class NullnessPropagationTransferCases2 {
  private static class MyClass {
    static String staticReturnNullable() {
      return null;
    }
  }

  static final int CONSTANT_INT = 1;
  static final Integer CONSTANT_BOXED_INTEGER = 1;
  static final Integer CONSTANT_DERIVED_INTEGER = (Integer) (CONSTANT_INT);
  static final Boolean CONSTANT_DERIVED_BOOLEAN = CONSTANT_INT == 1;
  static final String CONSTANT_STRING = "foo";
  static final String CONSTANT_NULL_STRING = null;
  static final String CONSTANT_DERIVED_STRING = CONSTANT_DERIVED_BOOLEAN ? CONSTANT_STRING : "";
  static final MyClass CONSTANT_OTHER_CLASS = new MyClass();
  static final Integer[] CONSTANT_OBJECT_ARRAY = new Integer[7];
  static final Integer[] CONSTANT_ARRAY_INITIALIZER = {Integer.valueOf(5)};
  static final Object CONSTANT_NO_INITIALIZER;

  static {
    CONSTANT_NO_INITIALIZER = new Object();
  }

  public void constants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(CONSTANT_INT);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_BOXED_INTEGER);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_DERIVED_INTEGER);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_DERIVED_BOOLEAN);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_STRING);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(CONSTANT_NULL_STRING);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_DERIVED_STRING);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_OTHER_CLASS);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_OBJECT_ARRAY);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_OBJECT_ARRAY[0]);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_ARRAY_INITIALIZER);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_NO_INITIALIZER);
  }

  public static final MyBigInteger CIRCULAR = MyBigInteger.CIRCLE;

  public void circularInitialization() {
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(MyBigInteger.CIRCLE);
  }

  static class MyBigInteger extends BigInteger {
    // Shadows BigInteger.ONE.
    public static final MyBigInteger ONE = null;
    // Creates circular initializer dependency.
    public static final MyBigInteger CIRCLE = NullnessPropagationTransferCases2.CIRCULAR;

    MyBigInteger(String val) {
      super(val);
    }

    // Has the same signature as a BigInteger method. In other words, our method hides that one.
    public static BigInteger valueOf(long val) {
      return null;
    }
  }

  public void builtInConstants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(BigInteger.ZERO);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyBigInteger.ZERO);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(BigInteger.ONE);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(MyBigInteger.ONE);
  }

  enum MyEnum {
    ENUM_INSTANCE;

    static MyEnum valueOf(char c) {
      return null;
    }

    public static final MyEnum NOT_COMPILE_TIME_CONSTANT = ENUM_INSTANCE;
    public static final MyEnum UNKNOWN_VALUE_CONSTANT = instance();

    public static MyEnum instance() {
      return ENUM_INSTANCE;
    }
  }

  public void enumConstants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyEnum.ENUM_INSTANCE);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(ENUM_INSTANCE);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyEnum.NOT_COMPILE_TIME_CONSTANT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.UNKNOWN_VALUE_CONSTANT);
  }

  public void explicitValueOf() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(String.valueOf(3));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(valueOf(3));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Integer.valueOf(null));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(BigInteger.valueOf(3));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Enum.valueOf(MyEnum.class, "INSTANCE"));

    // We'd prefer this to be Non-null. See the TODO on CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS.
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.valueOf("INSTANCE"));

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyBigInteger.valueOf(3));
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.valueOf('a'));
  }

  public void methodInvocationIsDereference(String nullableParam) {
    String str = nullableParam;
    str.toString();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
  }

  public void booleanMethodInvocationIsDereference(String nullableParam) {
    String str = nullableParam;
    str.isEmpty();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
  }

  public void staticMethodInvocationIsNotDereferenceNullableReturn(MyClass nullableParam) {
    nullableParam.staticReturnNullable();
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticMethodInvocationIsNotDereferenceNonNullReturn(String nullableParam) {
    nullableParam.valueOf(true);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticMethodInvocationIsNotDereferenceButPreservesExistingInformation() {
    String s = "foo";
    s.format("%s", "foo");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void staticMethodInvocationIsNotDereferenceButDefersToOtherNewInformation(String s) {
    s = s.valueOf(true);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void classgetNamesMethods() {
    Class<?> klass = this.getClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(klass.getName());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(klass.getSimpleName());
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(klass.getCanonicalName());
  }

  public void stringStaticMethodsReturnNonNull() {
    String s = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(String.format("%s", "foo"));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(format("%s", "foo"));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s.format("%s", "foo"));
  }

  public void stringInstanceMethodsReturnNonNull() {
    String s = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s.substring(0));
  }

  public void requireNonNullReturnsNonNull(String s) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Objects.requireNonNull(s));
  }

  public void requireNonNullUpdatesVariableNullness(String s) {
    Objects.requireNonNull(s);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void checkNotNullReturnsNonNull(String s) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(checkNotNull(s));
  }

  public void checkNotNullUpdatesVariableNullness(String s) {
    checkNotNull(s);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void verifyNotNullReturnsNonNull(String s) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(verifyNotNull(s));
  }

  public void verifyNotNullUpdatesVariableNullness(String s) {
    verifyNotNull(s);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void junit3AssertNotNullOneArgUpdatesVariableNullness(Object o) {
    junit.framework.Assert.assertNotNull(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
  }

  public void junit3AssertNotNullTwoArgUpdatesVariableNullness(String message, Object o) {
    junit.framework.Assert.assertNotNull(message, o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(message);
  }

  public void junit4AssertNotNullOneArgUpdatesVariableNullness(Object o) {
    org.junit.Assert.assertNotNull(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
  }

  public void junit4AssertNotNullTwoArgUpdatesVariableNullness(String message, Object o) {
    org.junit.Assert.assertNotNull(message, o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(message);
  }

  public void stringsIsNullOrEmptyIsNullCheck(String s) {
    if (Strings.isNullOrEmpty(s)) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(s);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(s);
    }
  }

  public void objectsIsNullIsNullCheck(String s) {
    if (Objects.isNull(s)) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(s);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(s);
    }
  }

  public void objectsNonNullIsNullCheck(String s) {
    if (Objects.nonNull(s)) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(s);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(s);
    }
  }

  public void optionalMethodsReturnNonNullUnlessAnnotated() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.absent());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.of(""));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.fromNullable(null));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.of("Test"));
    Optional<String> myOptional = Optional.absent();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(myOptional.get());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(myOptional.or(""));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(myOptional.asSet());

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(myOptional.orNull());
  }
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions3() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases3.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnBoxed;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;

import com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation around various
 * kinds of expressions, method parameter and call handling, and loops.
 */
public class NullnessPropagationTransferCases3 {
  public void casts() {
    Object o = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker((String) o);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed((int) o);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive((int) o);

    o = "str";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker((String) o);
  }

  public void literals() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive((byte) 1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive((short) 1000);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(33L);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(0.444f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(0.5555d);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(true);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive('z');
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker("a string literal");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(String.class);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(null);
  }

  public void autoboxed() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed((byte) 1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed((short) 1000);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(33L);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(0.444f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(0.5555d);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(true);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed('z');
  }

  public void autounbox() {
    Integer i = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
    // Unboxing is a method call, so i must be non-null...
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(i);
  }

  public void parameter(String str, int i) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    // A call to plain triggerNullnessChecker() would implicitly call Integer.valueOf(i).
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
  }

  public void assignment(String nullableParam) {
    String str = nullableParam;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    str = "a string";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    String otherStr = str;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    str = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);

    otherStr = str;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);
  }

  public void assignmentExpressionValue() {
    String str = "foo";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str = null);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str = "bar");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    str = null;
    String str2 = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str = str2 = "bar");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);

    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str = str2 = null);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void localVariable() {
    short s;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s = 1000); // narrowing conversion
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s);
    int i = 2;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i = s); // widening conversion
    String str = "a string literal";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    Object obj = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(obj);

    ++i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
  }

  public void boxedPrimitives() {
    Short s = 1000;
    // BUG: Diagnostic contains: (Non-null)
    NullnessPropagationTest.triggerNullnessChecker(s);

    Integer i = 2;
    // BUG: Diagnostic contains: (Non-null)
    NullnessPropagationTest.triggerNullnessChecker(i);
  }

  public void nullableAssignmentToPrimitiveVariableExpressionValue() {
    int i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i = boxedIntReturningMethod());
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(boxedIntReturningMethod());
  }

  public void methodInvocation() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(intReturningMethod());

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(stringReturningMethod());
  }

  private Integer boxedIntReturningMethod() {
    return null;
  }

  private int intReturningMethod() {
    return 0;
  }

  private String stringReturningMethod() {
    return null;
  }

  public void objectCreation(Object nullableParam) {
    Object obj = nullableParam;
    obj = new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(obj);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(new Object[0]);
  }

  public void inc() {
    int i = 0;
    short s = 0;

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i++);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s++);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(++i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(++s);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i += 5);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s += 5);
  }

  public void loop1() {
    Object o = null;
    while (true) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(o);
      o.hashCode();
    }
  }

  public void loop2() {
    Object o = null;
    Object comingValue = null;
    while (true) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(o);

      o = comingValue;
      comingValue = new Object();
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions4() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases4.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

/** Tests for ==. */
public class NullnessPropagationTransferCases4 {

  public void equalBothNull() {
    String str1 = null;
    if (str1 == null) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);

    if (null == str1) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void equalBothNonNull() {
    String str1 = "foo";
    if (str1 == "bar") {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    if ("bar" == str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    String str2 = "bar";
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  }

  public void equalOneNullOtherNonNull() {
    String str1 = "foo";
    if (str1 == null) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    if (null == str1) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);

    if (str2 == str1) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void equalOneNullableOtherNull(String nullableParam) {
    String str1 = nullableParam;
    if (str1 == null) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    if (null == str1) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);

    if (str2 == str1) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void equalOneNullableOtherNonNull(String nullableParam) {
    String str1 = nullableParam;
    if (str1 == "foo") {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    if ("foo" == str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    String str2 = "foo";
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);

    if (str2 == str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  }

  // TODO(eaftan): tests for bottom?
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions5() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases5.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

/** Tests for !=. */
public class NullnessPropagationTransferCases5 {

  public void notEqualBothNull() {
    String str1 = null;
    if (str1 != null) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);

    if (null != str1) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 != str2) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void notEqualBothNonNull() {
    String str1 = "foo";
    if (str1 != "bar") {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    if ("bar" != str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    String str2 = "bar";
    if (str1 != str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  }

  public void notEqualOneNullOtherNonNull() {
    String str1 = "foo";
    if (str1 != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    if (null != str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 != str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);

    if (str2 != str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void notEqualOneNullableOtherNull(String nullableParam) {
    String str1 = nullableParam;
    if (str1 != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    if (null != str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 != str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);

    if (str2 != str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void notEqualOneNullableOtherNonNull(String nullableParam) {
    String str1 = nullableParam;
    if (str1 != "foo") {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    if ("foo" != str1) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);

    String str2 = "foo";
    if (str1 != str2) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);

    if (str2 != str1) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  }

  // TODO(eaftan): tests for bottom?
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions6() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases6.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;
import static com.google.errorprone.dataflow.nullnesspropagation.testdata.NullnessPropagationTransferCases6.MyEnum.ENUM_INSTANCE;

/**
 * Tests for:
 *
 * <ul>
 *   <li>bitwise operations
 *   <li>numerical operations and comparisons
 *   <li>plain {@code visitNode}
 *   <li>name shadowing
 * </ul>
 */
public class NullnessPropagationTransferCases6 {
  enum MyEnum {
    ENUM_INSTANCE;
  }

  public void bitwiseOperations() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 | 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 & 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 ^ 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(~1);
  }

  public void bitwiseOperationsAreDereferences(Integer i) {
    /*
     * This next part has nothing to do with bitwise operations per se. The reason that it works is
     * that we recognize the implicit intValue() call as a dereference.
     */
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(i);
    int unused = ~i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(i);
  }

  public void numercialOperations() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 + 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 - 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 * 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 / 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 % 2);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1.0 / 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1.0 % 2);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 << 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 >> 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 >>> 2);

    int i = 1;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(+i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(-i);
  }

  public void numericalOperationsAreDereferences(Integer i) {
    // See bitwiseOperationsAreDereferences for some background.

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(i);
    int unused = i + i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(i);
  }

  public void numercialComparisons() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 < 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 > 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 <= 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 >= 2);
  }

  public void numericalComparisonsAreDereferences(Integer a, Integer b) {
    // See bitwiseOperationsAreDereferences for some background.

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(a);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(b);
    int unused = a + b;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(b);
  }

  public void vanillaVisitNode() {
    String[] a = new String[1];
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(a[0]);
  }

  public void sameNameImmediatelyShadowed() {
    final String s = "foo";

    class Bar {
      void method(String s) {
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(s);
      }
    }
  }

  public void sameNameLaterShadowed() {
    final String s = "foo";

    class Bar {
      void method() {
        // BUG: Diagnostic contains: (Non-null)
        triggerNullnessChecker(s);

        String s = HasStaticFields.staticStringField;
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(s);
      }
    }
  }

  public void sameNameShadowedThenUnshadowed() {
    final String s = HasStaticFields.staticStringField;

    class Bar {
      void method() {
        {
          String s = "foo";
          // BUG: Diagnostic contains: (Non-null)
          triggerNullnessChecker(s);
        }

        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(s);
      }
    }
  }

  public void nonCompileTimeConstantCapturedVariable() {
    final Object nonnull = ENUM_INSTANCE;

    class Bar {
      void method() {
        /*
         * We'd prefer for this to be non-null, but we don't run the analysis over the enclosing
         * class's enclosing method, so our captured-variable handling is limited to compile-time
         * constants, which include only primitives and strings:
         * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.28
         */
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(nonnull);
      }
    }
  }

  static class HasStaticFields {
    static String staticStringField;
  }
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions7() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases7.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnBoxed;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;
import static com.google.errorprone.dataflow.nullnesspropagation.testdata.NullnessPropagationTransferCases7.HasStaticFields.staticIntField;
import static com.google.errorprone.dataflow.nullnesspropagation.testdata.NullnessPropagationTransferCases7.HasStaticFields.staticStringField;

/** Tests for field accesses and assignments. */
public class NullnessPropagationTransferCases7 {
  private static class MyClass {
    int field;
  }

  private static class MyContainerClass {
    MyClass field;
  }

  enum MyEnum {
    ENUM_INSTANCE;
  }

  static class HasStaticFields {
    static String staticStringField;
    static int staticIntField;
  }

  private int i;
  private String str;
  private Object obj;

  private Integer boxedIntReturningMethod() {
    return null;
  }

  public void field() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(i);

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(obj);
  }

  public void fieldQualifiedByThis() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(this.i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(this.i);
  }

  public void fieldQualifiedByOtherVar() {
    NullnessPropagationTransferCases7 self = this;

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(self.i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(self.i);
  }

  public void fieldAccessIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    int i = mc.field;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(mc);
  }

  public void staticFieldAccessIsNotDereferenceNullableReturn(HasStaticFields nullableParam) {
    String s = nullableParam.staticStringField;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticFieldAccessIsNotDereferenceNonNullReturn(MyEnum nullableParam) {
    MyEnum x = nullableParam.ENUM_INSTANCE;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void fieldAssignmentIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    mc.field = 0;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(mc);
  }

  public void chainedFieldAssignmentIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    MyContainerClass container = new MyContainerClass();
    container.field.field = 0;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container);
  }

  public void staticFieldAssignmentIsNotDereferenceNullableReturn(HasStaticFields nullableParam) {
    nullableParam.staticStringField = "foo";
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticFieldAssignmentIsNotDereferenceNonNullReturn(HasStaticFields nullableParam) {
    nullableParam.staticIntField = 0;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticFieldAccessIsNotDereferenceButPreservesExistingInformation() {
    HasStaticFields container = new HasStaticFields();
    String s = container.staticStringField;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container);
  }

  public void trackFieldValues() {
    MyContainerClass container = new MyContainerClass();
    container.field = new MyClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container.field);

    container.field.field = 10;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container.field);
  }

  public void assignmentToFieldExpressionValue() {
    MyContainerClass container = new MyContainerClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container.field = new MyClass());
  }

  public void assignmentToPrimitiveFieldExpressionValue() {
    MyClass mc = new MyClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(mc.field = 10);
  }

  public void assignmentToStaticImportedFieldExpressionValue() {
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(staticStringField = null);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(staticStringField = "foo");
  }

  public void assignmentToStaticImportedPrimitiveFieldExpressionValue() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(staticIntField = boxedIntReturningMethod());
  }

  public void nullableAssignmentToPrimitiveFieldExpressionValue() {
    MyClass mc = new MyClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(mc.field = boxedIntReturningMethod());
  }
}\
""")
        .doTest();
  }

  @Test
  public void transferFunctions8() {
    compilationHelper
        .addSourceLines(
            "NullnessPropagationTransferCases8.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/** Tests for {@code try} blocks. */
public class NullnessPropagationTransferCases8 {
  public void caughtException() {
    try {
      System.out.println();
    } catch (Throwable t) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(t);

      t = something();
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(t);
    }
  }

  void tryWithResources() throws Exception {
    try (OutputStream out = something()) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(out);
    }

    try (OutputStream out = new ByteArrayOutputStream()) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(out);
    }
  }

  <T> T something() {
    return null;
  }
}\
""")
        .doTest();
  }

  @Test
  public void nonNullThis() {
    compilationHelper
        .addSourceLines(
            "ThisNonNullTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class ThisNonNullTest {
  public void instanceMethod() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(this);
  }
}
""")
        .doTest();
  }

  @Test
  public void equals() {
    compilationHelper
        .addSourceLines(
            "ThisEqualsTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class ThisEqualsTest {
  @Override
  public boolean equals(Object obj) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(obj);
    return this == obj;
  }

  private void testEquals(Object arg) {
    ThisEqualsTest thisEqualsTest = new ThisEqualsTest();
    if (thisEqualsTest.equals(arg)) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(arg);
    }
  }
}
""")
        .doTest();
  }

  @Test
  public void instanceofNonNull() {
    compilationHelper
        .addSourceLines(
            "InstanceofTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class InstanceofTest {
  public static void m(Object o) {
    if (o instanceof InstanceofTest) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(o);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(o);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o);
  }
}
""")
        .doTest();
  }

  @Test
  public void protoGetters() {
    compilationHelper
        .addSourceLines(
            "InstanceofTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

public class InstanceofTest {
  public static void m(TestProtoMessage o) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.getMessage());
  }
}
""")
        .doTest();
  }

  @Test
  public void arrayAccess() {
    compilationHelper
        .addSourceLines(
            "ArrayAccessTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class ArrayAccessTest {
  public static void read(Integer[] a) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(a);
    Integer result = a[0];
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(result);
  }

  public static void read(int[][] matrix) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(matrix);
    int result = matrix[0][0];
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(matrix);
  }

  public static void write(int[] vector) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(vector);
    vector[7] = 42;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(vector);
  }
}
""")
        .doTest();
  }

  @Test
  public void fieldAccess() {
    compilationHelper
        .addSourceLines(
            "FieldAccessTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class FieldAccessTest {
  public static void dereference(Coinductive o) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o.f);
    o.f = (Coinductive) new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f);
  }

  abstract class Coinductive {
    Coinductive f;
  }
}
""")
        .doTest();
  }

  @Test
  public void fieldReceivers() {
    compilationHelper
        .addSourceLines(
            "FieldReceiversTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class FieldReceiversTest {
  Object f;

  public FieldReceiversTest getSelf() {
    return this;
  }

  public void test_different_receivers(FieldReceiversTest other) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(other);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(this);
    other.f = new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(other.f);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(this.f);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(f);
    this.f = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(this.f);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(f);
  }
}
""")
        .doTest();
  }

  @Test
  public void fieldPathSensitivity() {
    compilationHelper
        .addSourceLines(
            "FieldPathSensitivityTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class FieldPathSensitivityTest {
  public static void path_sensitivity(Coinductive o) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o.f);
    if (o.f == null) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(o.f);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(o.f);
    }
  }

  abstract class Coinductive {
    Coinductive f;
  }
}
""")
        .doTest();
  }

  @Test
  public void accessPaths() {
    compilationHelper
        .addSourceLines(
            "AccessPathsTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class AccessPathsTest {
  public static void access_paths(Coinductive o) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o.f.f.f.f.f);
    o.f.f.f.f.f = (Coinductive) new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f.f.f.f.f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f.f.f.f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f.f.f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f.f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f);
  }

  abstract class Coinductive {
    Coinductive f;
  }
}
""")
        .doTest();
  }

  @Test
  public void untrackableFields() {
    compilationHelper
        .addSourceLines(
            "UntrackableFieldsTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class UntrackableFieldsTest {
  public static void untrackable_fields(CoinductiveWithMethod o) {
    o.f.f = (CoinductiveWithMethod) new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o.f.f);
    o.foo().f = (CoinductiveWithMethod) new Object();
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o.foo().f);
  }

  abstract class CoinductiveWithMethod {
    CoinductiveWithMethod f;

    abstract CoinductiveWithMethod foo();
  }
}
""")
        .doTest();
  }

  @Test
  public void annotatedAtGenericTypeUse() {
    compilationHelper
        .addSourceLines(
            "AnnotatedAtGenericTypeUseTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AnnotatedAtGenericTypeUseTest {
  void test(MyInnerClass<@Nullable Object> nullable, MyInnerClass<@NonNull Object> nonnull) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullable.get());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(nonnull.get());
  }

  interface MyInnerClass<T> {
    T get();
  }
}
""")
        .doTest();
  }

  @Test
  public void annotatedAtGenericTypeDef() {
    compilationHelper
        .addSourceLines(
            "AnnotatedAtGenericTypeDefTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedAtGenericTypeDefTest {",
            "  void test(NullableTypeInner<?> nullable,"
                + "NonNullTypeInner<?> nonnull,"
                + "TypeAndBoundInner<?> type_and_bound) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.get());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nullable.getNonNull());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.get());",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.getNullable());",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.getNullableDecl());",
            "    // BUG: Diagnostic contains: (Nullable)", // use upper bound (b/121398981)
            "    triggerNullnessChecker(type_and_bound.get());",
            "  }",
            "  interface NullableTypeInner<@Nullable T> {",
            "    T get();",
            "    @NonNull T getNonNull();",
            "  }",
            "  interface NonNullTypeInner<@NonNull T> {",
            "    T get();",
            "    @Nullable T getNullable();",
            "    @NullableDecl T getNullableDecl();",
            "  }",
            "  interface TypeAndBoundInner<@NonNull T extends @Nullable Object> { T get(); }",
            "}")
        .doTest();
  }

  @Test
  public void boundedAtGenericTypeUse() {
    compilationHelper
        .addSourceLines(
            "BoundedAtGenericTypeUseTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BoundedAtGenericTypeUseTest {
  void test(
      MyInnerClass<? extends @Nullable Object> nullable,
      MyInnerClass<? extends @NonNull Object> nonnull) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullable.get());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(nullable.getNonNull());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(nonnull.get());
  }

  interface MyInnerClass<T> {
    T get();

    @NonNull T getNonNull();
  }
}
""")
        .doTest();
  }

  @Test
  public void boundedAtGenericTypeDef() {
    compilationHelper
        .addSourceLines(
            "BoundedAtGenericTypeDefTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BoundedAtGenericTypeDefTest {
  void test(NullableElementCollection<?> nullable, NonNullElementCollection<?> nonnull) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullable.get());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(nonnull.get());
  }

  interface NullableElementCollection<T extends @Nullable Object> {
    T get();
  }

  interface NonNullElementCollection<T extends @NonNull Object> {
    T get();
  }
}
""")
        .doTest();
  }

  @Test
  public void annotatedMethodTypeParams() {
    compilationHelper
        .addSourceLines(
            "AnnotatedMethodTypeParamsTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AnnotatedMethodTypeParamsTest {
  public void test() {
    Object o = new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(AnnotatedMethodTypeParamsTest.<@NonNull Object>id(o));
  }

  static <T> T id(T t) {
    return t;
  }
}
""")
        .doTest();
  }

  @Test
  public void fieldAnnotations() {
    compilationHelper
        .addSourceLines(
            "FieldAnnotationsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class FieldAnnotationsTest {",
            "  void test(NullableTypeInner<?> nullable,"
                + "NonNullTypeInner<?> nonnull,"
                + "TypeAndBoundInner<?> type_and_bound) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.foo);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.foo);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.fooNullable);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.fooNullableDecl);",
            "    // BUG: Diagnostic contains: (Nullable)", // use upper bound (b/121398981)
            "    triggerNullnessChecker(type_and_bound.foo);",
            "  }",
            "  class NullableTypeInner<@Nullable T> { T foo; }",
            "  class NonNullTypeInner<@NonNull T> {",
            "    T foo;",
            "    @Nullable T fooNullable;",
            "    @NullableDecl T fooNullableDecl;",
            "  }",
            "  class TypeAndBoundInner<@NonNull T extends @Nullable Object> { T foo; }",
            "}")
        .doTest();
  }

  @Test
  public void checkerWorksInsideLambdaBody() {
    compilationHelper
        .addSourceLines(
            "LambdaBodyTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class LambdaBodyTest {
  public void startNothing() {
    new Thread(
            ()
            // BUG: Diagnostic contains: (Null)
            -> triggerNullnessChecker(null))
        .start();
  }
}
""")
        .doTest();
  }

  @Test
  public void checkerWorksInsideInitializer() {
    compilationHelper
        .addSourceLines(
            "InitializerBlockTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class InitializerBlockTest {",
            "  // BUG: Diagnostic contains: (Null)",
            "  Object o1 = triggerNullnessChecker(null);", // instance field initializer
            "  // BUG: Diagnostic contains: (Null)",
            "  static Object o2 = triggerNullnessChecker(null);", // static field initializer
            "  {", // instance initializer block
            "    // BUG: Diagnostic contains: (Null)",
            "    triggerNullnessChecker(null);",
            "  }",
            "  static {", // static initializer block
            "    // BUG: Diagnostic contains: (Null)",
            "    triggerNullnessChecker(null);",
            "    // The following is a regression test for b/80179088",
            "    int i = 0; i = i++;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Tests nullness propagation for references to constants defined in other compilation units. Enum
   * constants and compile-time constants are still known to be non-null; other constants are
   * assumed nullable. It doesn't matter whether the referenced compilation unit is part of the same
   * compilation or not. Note we often do better when constants are defined in the same compilation
   * unit. Circular initialization dependencies between compilation units are also not recognized
   * while we do recognize them inside a compilation unit.
   */
  @Test
  public void constantsDefinedInOtherCompilationUnits() {
    compilationHelper
        .addSourceLines(
            "AnotherEnum.java",
            """
            package com.google.errorprone.dataflow.nullnesspropagation;

            public enum AnotherEnum {
              INSTANCE;
              public static final String COMPILE_TIME_CONSTANT = "not null";
              public static final AnotherEnum NOT_COMPILE_TIME_CONSTANT = INSTANCE;
              public static final String CIRCULAR = ConstantsFromOtherCompilationUnits.CIRCULAR;
            }
            """)
        .addSourceLines(
            "ConstantsFromOtherCompilationUnits.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class ConstantsFromOtherCompilationUnits {
  public static final String CIRCULAR = AnotherEnum.CIRCULAR;

  public void referenceInsideCompilation() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(AnotherEnum.INSTANCE);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(AnotherEnum.COMPILE_TIME_CONSTANT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(AnotherEnum.NOT_COMPILE_TIME_CONSTANT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CIRCULAR);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(AnotherEnum.CIRCULAR);
  }

  public void referenceOutsideCompilation() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(NullnessPropagationTest.COMPILE_TIME_CONSTANT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(NullnessPropagationTest.NOT_COMPILE_TIME_CONSTANT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(System.out);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(java.math.RoundingMode.UNNECESSARY);
  }
}
""")
        .doTest();
  }

  // Regression test for b/110756716, verifying that the l-val of an assignment in expr position in
  // an equality comparison is refined
  @Test
  public void whileLoopPartialCorrectness() {
    compilationHelper
        .addSourceLines(
            "PartialCorrectnessTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

public class PartialCorrectnessTest {
  public void test(java.util.function.Supplier<Object> supplier) {
    Object result;
    while ((result = supplier.get()) == null) {}
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(result);
  }
}
""")
        .doTest();
  }

  @Test
  public void casts() {
    compilationHelper
        .addSourceLines(
            "CastsTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CastsTest {
  public void test(@Nullable Object o) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker((@NonNull Object) o);
  }
}
""")
        .doTest();
  }

  @Test
  public void autoValue() {
    compilationHelper
        .addSourceLines(
            "AutoValueTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import com.google.auto.value.AutoValue;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AutoValueTest {
  public void test(Value v) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(v.accessor().field);
    if (v.accessor().field != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(v.accessor().field);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(v.nullableAccessor());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(v.accessor());
  }

  @AutoValue
  abstract static class Value {
    Value field;

    abstract Value accessor();

    @Nullable
    abstract Value nullableAccessor();
  }
}
""")
        .doTest();
  }

  @Test
  public void genericTypeInference() {
    compilationHelper
        .addSourceLines(
            "GenericTypeInferenceTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericTypeInferenceTest {
  public void test(@NonNull Object o) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(id(o));
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(idButMaybeNullify(o));
  }

  <T> T id(T t) {
    return t;
  }

  <T> @Nullable T idButMaybeNullify(T t) {
    return java.lang.Math.random() > 0.5 ? t : null;
  }
}
""")
        .doTest();
  }

  @Test
  public void annotatedFormal() {
    compilationHelper
        .addSourceLines(
            "AnnotatedFormalTest.java",
"""
package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnnotatedFormalTest {
  public void test(@NonNull Object nonnull, @Nullable Object nullable, Object o) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(nonnull);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullable);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(o);
  }
}
""")
        .doTest();
  }

  /** BugPattern to test dataflow analysis using nullness propagation */
  @BugPattern(
      summary = "Test checker for NullnessPropagationTest",
      explanation =
          "Outputs an error for each call to triggerNullnessChecker, describing its "
              + "argument as nullable or non-null",
      severity = ERROR)
  public static final class NullnessPropagationChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    private final NullnessPropagationTransfer nullnessPropagation =
        new NullnessPropagationTransfer();

    private static final String AMBIGUOUS_CALL_MESSAGE =
        "AMBIGUOUS CALL: use "
            + "triggerNullnessCheckerOnPrimitive if you want to test the primitive for nullness";

    private static final Matcher<ExpressionTree> TRIGGER_CALL_MATCHER =
        anyOf(
            staticMethod()
                .onClass(NullnessPropagationTest.class.getName())
                .named("triggerNullnessCheckerOnPrimitive"),
            staticMethod()
                .onClass(NullnessPropagationTest.class.getName())
                .named("triggerNullnessCheckerOnBoxed"),
            staticMethod()
                .onClass(NullnessPropagationTest.class.getName())
                .named("triggerNullnessChecker")
                .withParameters("java.lang.Object"));

    private static final Matcher<ExpressionTree> AMBIGUOUS_CALL_FALLBACK_MATCHER =
        staticMethod()
            .onClass(NullnessPropagationTest.class.getName())
            .named("triggerNullnessChecker");

    @Override
    public Description matchMethodInvocation(
        MethodInvocationTree methodInvocation, VisitorState state) {
      if (!TRIGGER_CALL_MATCHER.matches(methodInvocation, state)) {
        if (AMBIGUOUS_CALL_FALLBACK_MATCHER.matches(methodInvocation, state)) {
          return buildDescription(methodInvocation).setMessage(AMBIGUOUS_CALL_MESSAGE).build();
        }
        return NO_MATCH;
      }

      TreePath root = state.getPath();
      List<Object> values = new ArrayList<>();
      for (Tree arg : methodInvocation.getArguments()) {
        TreePath argPath = new TreePath(root, arg);
        nullnessPropagation.setContext(state.context).setCompilationUnit(root.getCompilationUnit());
        values.add(expressionDataflow(argPath, state.context, nullnessPropagation));
        nullnessPropagation.setContext(null).setCompilationUnit(null);
      }

      String fixString = "(" + Joiner.on(", ").join(values) + ")";
      return describeMatch(methodInvocation, replace(methodInvocation, fixString));
    }
  }
}
