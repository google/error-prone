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

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author awturner@google.com (Andy Turner)
 */
@RunWith(JUnit4.class)
public class UnnecessaryBoxedVariableTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryBoxedVariable.class, getClass());
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(UnnecessaryBoxedVariable.class, getClass());

  @Test
  public void cases() {
    helper
        .addInputLines(
            "UnnecessaryBoxedVariableCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.base.Preconditions.checkNotNull;

            import java.util.List;
            import java.util.stream.Stream;
            import org.jspecify.annotations.Nullable;

            /**
             * @author awturner@google.com (Andy Turner)
             */
            class UnnecessaryBoxedVariableCases {
              void positive_local() {
                Integer i = 0;
              }

              int positive_local_return() {
                Integer i = 0;
                return i;
              }

              Integer positive_local_addition() {
                Integer i = 0;
                return i + 1;
              }

              void positive_local_compoundAddition(Integer addend) {
                Integer i = 0;
                i += addend;

                int j = 0;
                j += i;
              }

              void positive_methodInvocation() {
                Integer i = 0;
                methodPrimitiveArg(i);
              }

              void negative_methodInvocation() {
                Integer i = 0;
                methodBoxedArg(i);
              }

              void positive_assignedValueOf() {
                Integer i = Integer.valueOf(0);
              }

              int positive_assignedValueOf_return() {
                Integer i = Integer.valueOf(0);
                return i;
              }

              int positive_noInitializer() {
                Integer i;
                i = 0;
                return i;
              }

              void negative_enhancedForLoopOverCollection(List<Integer> list) {
                for (Integer i : list) {}
              }

              void negative_enhancedForLoopOverWrappedArray(Integer[] array) {
                for (Integer i : array) {}
              }

              void positive_enhancedForLoopOverPrimitiveArray(int[] array) {
                for (Integer i : array) {}
              }

              final void negative_invokeMethod(Integer i) throws InterruptedException {
                i.wait(0);
              }

              final Object[] negative_objectArray(Long l) {
                return new Object[] {"", l};
              }

              void negative_null() {
                Integer i = null;
              }

              void negative_null_noInitializer() {
                Integer i;
                i = null;
                i = 0;
              }

              void negative_null_reassignNull() {
                Integer i = 0;
                i = null;
              }

              void negative_enhancedForLoopOverPrimitiveArray_assignInLoop(int[] array) {
                for (Integer i : array) {
                  i = null;
                }
              }

              void negative_boxedVoid() {
                Void v;
              }

              int negative_assignmentInReturn() {
                Integer myVariable;
                return myVariable = methodBoxedArg(42);
              }

              int positive_assignmentInReturn() {
                Integer myVariable;
                return myVariable = Integer.valueOf(42);
              }

              int positive_assignmentInReturn2() {
                Integer myVariable;
                return myVariable = Integer.valueOf(42);
              }

              int positive_hashCode() {
                Integer myVariable = 0;
                return myVariable.hashCode();
              }

              short positive_castMethod() {
                Integer myVariable = 0;
                return myVariable.shortValue();
              }

              int positive_castMethod_sameType() {
                Integer myVariable = 0;
                return myVariable.intValue();
              }

              void positive_castMethod_statementExpression() {
                Integer myVariable = 0;
                myVariable.longValue();
              }

              void negative_methodReference() {
                Integer myVariable = 0;
                Stream<Integer> stream = Stream.of(1).filter(myVariable::equals);
              }

              static void positive_parameter_staticMethod(Boolean b) {
                boolean a = b;
              }

              static void negative_parameter_staticMethod(Boolean b) {
                System.out.println("a " + b);
              }

              static boolean positive_parameter_returnType(Boolean b) {
                return b;
              }

              void negative_parameter_instanceMethod_nonFinal(Boolean b) {
                boolean a = b;
              }

              final void negative_parameter_instanceMethod_final(Boolean b) {
                boolean a = b;
              }

              static void negative_parameter_unused(Integer i) {}

              static void positive_removeNullable_parameter(@Nullable Integer i) {
                int j = i;
              }

              static void positive_removeNullable_localVariable() {
                @Nullable Integer i = 0;
                @org.jspecify.annotations.Nullable Integer j = 0;
                int k = i + j;
              }

              static int positive_nullChecked_expression(Integer i) {
                return checkNotNull(i);
              }

              static int positive_nullChecked_expression_message(Integer i) {
                return checkNotNull(i, "Null: [%s]", i);
              }

              static int positive_nullChecked_statement(Integer i) {
                checkNotNull(i);
                return i;
              }

              static int positive_nullChecked_statement_message(Integer i) {
                checkNotNull(i, "Null: [%s]", i);
                return i;
              }

              private void methodPrimitiveArg(int i) {}

              private Integer methodBoxedArg(Integer i) {
                return i;
              }
            }
            """)
        .addOutputLines(
            "UnnecessaryBoxedVariableCases_expected.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.base.Preconditions.checkNotNull;

            import java.util.List;
            import java.util.stream.Stream;
            import org.jspecify.annotations.Nullable;

            /**
             * @author awturner@google.com (Andy Turner)
             */
            class UnnecessaryBoxedVariableCases {
              void positive_local() {
                int i = 0;
              }

              int positive_local_return() {
                int i = 0;
                return i;
              }

              Integer positive_local_addition() {
                int i = 0;
                return i + 1;
              }

              void positive_local_compoundAddition(Integer addend) {
                int i = 0;
                i += addend;

                int j = 0;
                j += i;
              }

              void positive_methodInvocation() {
                int i = 0;
                methodPrimitiveArg(i);
              }

              void negative_methodInvocation() {
                Integer i = 0;
                methodBoxedArg(i);
              }

              void positive_assignedValueOf() {
                int i = 0;
              }

              int positive_assignedValueOf_return() {
                int i = 0;
                return i;
              }

              int positive_noInitializer() {
                int i;
                i = 0;
                return i;
              }

              void negative_enhancedForLoopOverCollection(List<Integer> list) {
                for (Integer i : list) {}
              }

              void negative_enhancedForLoopOverWrappedArray(Integer[] array) {
                for (Integer i : array) {}
              }

              void positive_enhancedForLoopOverPrimitiveArray(int[] array) {
                for (int i : array) {}
              }

              final void negative_invokeMethod(Integer i) throws InterruptedException {
                i.wait(0);
              }

              final Object[] negative_objectArray(Long l) {
                return new Object[] {"", l};
              }

              void negative_null() {
                Integer i = null;
              }

              void negative_null_noInitializer() {
                Integer i;
                i = null;
                i = 0;
              }

              void negative_null_reassignNull() {
                Integer i = 0;
                i = null;
              }

              void negative_enhancedForLoopOverPrimitiveArray_assignInLoop(int[] array) {
                for (Integer i : array) {
                  i = null;
                }
              }

              void negative_boxedVoid() {
                Void v;
              }

              int negative_assignmentInReturn() {
                Integer myVariable;
                return myVariable = methodBoxedArg(42);
              }

              int positive_assignmentInReturn() {
                int myVariable;
                return myVariable = 42;
              }

              int positive_assignmentInReturn2() {
                int myVariable;
                return myVariable = 42;
              }

              int positive_hashCode() {
                int myVariable = 0;
                return Integer.hashCode(myVariable);
              }

              short positive_castMethod() {
                int myVariable = 0;
                return (short) myVariable;
              }

              int positive_castMethod_sameType() {
                int myVariable = 0;
                return myVariable;
              }

              void positive_castMethod_statementExpression() {
                int myVariable = 0;
              }

              void negative_methodReference() {
                Integer myVariable = 0;
                Stream<Integer> stream = Stream.of(1).filter(myVariable::equals);
              }

              static void positive_parameter_staticMethod(boolean b) {
                boolean a = b;
              }

              static void negative_parameter_staticMethod(Boolean b) {
                System.out.println("a " + b);
              }

              static boolean positive_parameter_returnType(boolean b) {
                return b;
              }

              void negative_parameter_instanceMethod_nonFinal(Boolean b) {
                boolean a = b;
              }

              final void negative_parameter_instanceMethod_final(boolean b) {
                boolean a = b;
              }

              static void negative_parameter_unused(Integer i) {}

              static void positive_removeNullable_parameter(int i) {
                int j = i;
              }

              static void positive_removeNullable_localVariable() {
                int i = 0;
                int j = 0;
                int k = i + j;
              }

              static int positive_nullChecked_expression(int i) {
                return i;
              }

              static int positive_nullChecked_expression_message(int i) {
                return i;
              }

              static int positive_nullChecked_statement(int i) {

                return i;
              }

              static int positive_nullChecked_statement_message(int i) {

                return i;
              }

              private void methodPrimitiveArg(int i) {}

              private Integer methodBoxedArg(Integer i) {
                return i;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppression() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              @SuppressWarnings("UnnecessaryBoxedVariable")
              private int a(Integer o) {
                return o;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void lambdas() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              interface Boxed<O> {
                void a(O b);
              }

              void boxed(Boxed<?> b) {}

              private void test() {
                boxed(
                    (Double a) -> {
                      double b = a + 1;
                    });
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaReturn() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              interface F {
                int f(Integer i);
              }

              Test() {
                F f =
                    (Integer i) -> {
                      return i;
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveFactory() {
    compilationTestHelper
        .addSourceLines(
            "Foo.java",
            """
            import com.google.auto.value.AutoValue;

            @AutoValue
            abstract class Foo {
              abstract int getFoo();

              abstract boolean isBar();

              // BUG: Diagnostic contains: int foo
              static Foo create(Integer foo, Boolean bar) {
                return new AutoValue_Foo(foo, bar);
              }
            }
            """)
        .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void recordsIgnored() {
    compilationTestHelper
        .addSourceLines(
            "Foo.java",
            """
            import static com.google.common.base.Preconditions.checkNotNull;

            record Foo(Integer foo, boolean bar) {
              Foo {
                checkNotNull(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstant_alwaysUsedUnboxed_refactored() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: int FOO
              private static final Integer FOO = 42;

              public int foo() {
                return FOO;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void publicConstant_alwaysUsedUnboxed_notRefactored() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static final Integer FOO = 42;

              public int foo() {
                return FOO;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstant_usedBoxed_notRefactored() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static final Integer FOO = 42;

              public Integer foo() {
                return FOO;
              }

              public int unboxedFoo() {
                return FOO;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstant_usedInAnotherBoxedContext_notRefactored() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              private static final Integer FOO = 42;

              public ImmutableList<Integer> foos() {
                return ImmutableList.of(FOO);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void var() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private int a() {
                var y = Integer.valueOf(42);
                return y;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private int a() {
                int y = 42;
                return y;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assignment() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private void a() {
                Integer y = Integer.valueOf("42");
                y = Integer.valueOf("43");
                y = Integer.valueOf(43);
                y = new Integer("44");
                y = new Integer(44);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private void a() {
                int y = Integer.parseInt("42");
                y = Integer.parseInt("43");
                y = 43;
                y = Integer.parseInt("44");
                y = 44;
              }
            }
            """)
        .doTest();
  }
}
