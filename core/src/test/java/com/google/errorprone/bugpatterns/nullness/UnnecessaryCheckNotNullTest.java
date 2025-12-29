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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link UnnecessaryCheckNotNull} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class UnnecessaryCheckNotNullTest extends CompilerBasedAbstractTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnnecessaryCheckNotNull.class, getClass());

  @Test
  public void positive_newClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;
            import com.google.common.base.Verify;
            import java.util.Objects;

            class Test {
              void positive_checkNotNull() {
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Preconditions.checkNotNull(new String(""));
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Preconditions.checkNotNull(new String(""), new Object());
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Preconditions.checkNotNull(new String(""), "Message %s", "template");
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String pa = Preconditions.checkNotNull(new String(""));
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String pb = Preconditions.checkNotNull(new String(""), new Object());
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String pc = Preconditions.checkNotNull(new String(""), "Message %s", "template");
              }

              void positive_verifyNotNull() {
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Verify.verifyNotNull(new String(""));
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Verify.verifyNotNull(new String(""), "Message");
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Verify.verifyNotNull(new String(""), "Message %s", "template");
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String va = Verify.verifyNotNull(new String(""));
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String vb = Verify.verifyNotNull(new String(""), "Message");
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String vc = Verify.verifyNotNull(new String(""), "Message %s", "template");
              }

              void positive_requireNonNull() {
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Objects.requireNonNull(new String(""));
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Objects.requireNonNull(new String(""), "Message");
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String va = Objects.requireNonNull(new String(""));
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                String vb = Objects.requireNonNull(new String(""), "Message");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_newArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;
            import com.google.common.base.Verify;
            import java.util.Objects;

            class Test {
              void positive_checkNotNull() {
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Preconditions.checkNotNull(new int[3]);
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Preconditions.checkNotNull(new int[] {1, 2, 3});
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Preconditions.checkNotNull(new int[5][2]);
              }

              void positive_verifyNotNull() {
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Verify.verifyNotNull(new int[3]);
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Verify.verifyNotNull(new int[] {1, 2, 3});
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Verify.verifyNotNull(new int[5][2]);
              }

              void positive_requireNonNull() {
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Objects.requireNonNull(new int[3]);
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Objects.requireNonNull(new int[] {1, 2, 3});
                // BUG: Diagnostic contains: UnnecessaryCheckNotNull
                Objects.requireNonNull(new int[5][2]);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;
            import com.google.common.base.Verify;
            import java.util.Objects;

            class Test {
              void negative() {
                Preconditions.checkNotNull(new String("").substring(0, 0));
                Verify.verifyNotNull(new String("").substring(0, 0));
                Objects.requireNonNull(new String("").substring(0, 0));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "UnnecessaryCheckNotNullPositiveCase.java",
            """
            package com.google.errorprone.bugpatterns.nullness.testdata;

            import static com.google.common.base.Preconditions.checkNotNull;
            import static com.google.common.base.Verify.verifyNotNull;
            import static java.util.Objects.requireNonNull;

            import com.google.common.base.Preconditions;
            import com.google.common.base.Verify;
            import java.util.Objects;

            public class UnnecessaryCheckNotNullPositiveCase {
              public void error_checkNotNull() {
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull("string literal");

                // BUG: Diagnostic contains: remove this line
                checkNotNull("string literal");

                String thing = null;
                // BUG: Diagnostic contains: (thing,
                checkNotNull("thing is null", thing);
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull("a string literal " + "that's got two parts", thing);
              }

              public void error_verifyNotNull() {
                // BUG: Diagnostic contains: remove this line
                Verify.verifyNotNull("string literal");

                // BUG: Diagnostic contains: remove this line
                verifyNotNull("string literal");

                String thing = null;
                // BUG: Diagnostic contains: (thing,
                verifyNotNull("thing is null", thing);
                // BUG: Diagnostic contains:
                Verify.verifyNotNull("a string literal " + "that's got two parts", thing);
              }

              public void error_requireNonNull() {
                // BUG: Diagnostic contains: remove this line
                Objects.requireNonNull("string literal");

                // BUG: Diagnostic contains: remove this line
                requireNonNull("string literal");

                String thing = null;
                // BUG: Diagnostic contains: (thing,
                requireNonNull("thing is null", thing);
                // BUG: Diagnostic contains:
                Objects.requireNonNull("a string literal " + "that's got two parts", thing);
              }

              public void error_fully_qualified_import_checkNotNull() {
                // BUG: Diagnostic contains: remove this line
                com.google.common.base.Preconditions.checkNotNull("string literal");
              }

              public void error_fully_qualified_import_verifyNotNull() {
                // BUG: Diagnostic contains: remove this line
                com.google.common.base.Verify.verifyNotNull("string literal");
              }

              public void error_fully_qualified_import_requireNonNull() {
                // BUG: Diagnostic contains: remove this line
                java.util.Objects.requireNonNull("string literal");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "UnnecessaryCheckNotNullNegativeCase.java",
            """
            package com.google.errorprone.bugpatterns.nullness.testdata;

            public class UnnecessaryCheckNotNullNegativeCase {
              public void go_checkNotNull() {
                Preconditions.checkNotNull("this is ok");
              }

              public void go_verifyNotNull() {
                Verify.verifyNotNull("this is ok");
              }

              public void go_requireNonNull() {
                Objects.requireNonNull("this is ok");
              }

              private static class Preconditions {
                static void checkNotNull(String string) {
                  System.out.println(string);
                }
              }

              private static class Verify {
                static void verifyNotNull(String string) {
                  System.out.println(string);
                }
              }

              private static class Objects {
                static void requireNonNull(String string) {
                  System.out.println(string);
                }
              }

              public void go() {
                Object testObj = null;
                com.google.common.base.Preconditions.checkNotNull(testObj, "this is ok");
                com.google.common.base.Verify.verifyNotNull(testObj, "this is ok");
                java.util.Objects.requireNonNull(testObj, "this is ok");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void primitivePositiveCases() {
    compilationHelper
        .addSourceLines(
            "UnnecessaryCheckNotNullPrimitivePositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.nullness.testdata;

            import static com.google.common.base.Preconditions.checkNotNull;

            import com.google.common.base.Preconditions;

            public class UnnecessaryCheckNotNullPrimitivePositiveCases {

              private Tester field = new Tester();

              public void test() {
                Object a = new Object();
                Object b = new Object();
                byte byte1 = 0;
                short short1 = 0;
                int int1 = 0, int2 = 0;
                long long1 = 0;
                float float1 = 0;
                double double1 = 0;
                boolean boolean1 = false, boolean2 = false;
                char char1 = 0;
                Tester tester = new Tester();

                // Do we detect all primitive types?

                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(byte1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(short1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(int1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(long1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(float1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(double1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(boolean1);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(char1);

                // Do we give the right suggested fix?

                // BUG: Diagnostic contains: boolean1 = boolean2;
                boolean1 = Preconditions.checkNotNull(boolean2);
                // BUG: Diagnostic contains: boolean1 = int1 == int2;
                boolean1 = Preconditions.checkNotNull(int1 == int2);
                // BUG: Diagnostic contains: checkState(tester.hasId())
                Preconditions.checkNotNull(tester.hasId());
                // BUG: Diagnostic contains: checkState(tester.hasId(), "Must have ID!")
                Preconditions.checkNotNull(tester.hasId(), "Must have ID!");
                // BUG: Diagnostic contains: checkState(tester.hasId(), "Must have %s!", "ID")
                Preconditions.checkNotNull(tester.hasId(), "Must have %s!", "ID");

                // Do we handle arguments that evaluate to a primitive type?

                // BUG: Diagnostic contains: Preconditions.checkNotNull(a)
                Preconditions.checkNotNull(a != null);
                // BUG: Diagnostic contains: Preconditions.checkNotNull(a)
                Preconditions.checkNotNull(a == null);
                // BUG: Diagnostic contains: checkState(int1 == int2)
                Preconditions.checkNotNull(int1 == int2);
                // BUG: Diagnostic contains: checkState(int1 > int2)
                Preconditions.checkNotNull(int1 > int2);
                // BUG: Diagnostic contains: remove this line
                Preconditions.checkNotNull(boolean1 ? int1 : int2);

                // Do we handle static imports?

                // BUG: Diagnostic contains: remove this line
                checkNotNull(byte1);
                // BUG: Diagnostic contains: 'checkState(tester.hasId())
                checkNotNull(tester.hasId());
              }

              public void test2(Tester arg) {
                Tester local = new Tester();
                // Do we correctly distinguish checkArgument from checkState?

                // BUG: Diagnostic contains: checkArgument(arg.hasId())
                checkNotNull(arg.hasId());
                // BUG: Diagnostic contains: checkState(field.hasId())
                checkNotNull(field.hasId());
                // BUG: Diagnostic contains: checkState(local.hasId())
                checkNotNull(local.hasId());
                // BUG: Diagnostic contains: checkState(!local.hasId())
                checkNotNull(!local.hasId());

                // BUG: Diagnostic contains: checkArgument(!(arg instanceof Tester))
                checkNotNull(!(arg instanceof Tester));

                // BUG: Diagnostic contains: checkState(getTrue())
                checkNotNull(getTrue());

                // BUG: Diagnostic contains: remove this line
                checkNotNull(arg.getId());
                // BUG: Diagnostic contains: id = arg.getId()
                int id = checkNotNull(arg.getId());

                // BUG: Diagnostic contains: boolean b = arg.hasId();
                boolean b = checkNotNull(arg.hasId());

                // Do we handle long chains of method calls?

                // BUG: Diagnostic contains: checkArgument(arg.getTester().getTester().hasId())
                checkNotNull(arg.getTester().getTester().hasId());

                // BUG: Diagnostic contains: checkArgument(arg.tester.getTester().hasId())
                checkNotNull(arg.tester.getTester().hasId());
              }

              private boolean getTrue() {
                return true;
              }

              private static class Tester {
                public Tester tester;

                public boolean hasId() {
                  return true;
                }

                public int getId() {
                  return 10;
                }

                public Tester getTester() {
                  return tester;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void primitiveNegativeCases() {
    compilationHelper
        .addSourceLines(
            "UnnecessaryCheckNotNullPrimitiveNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.nullness.testdata;

            import static com.google.common.base.Preconditions.checkNotNull;

            import com.google.common.base.Preconditions;

            public class UnnecessaryCheckNotNullPrimitiveNegativeCases {
              public void test() {
                Object obj1 = new Object();

                Preconditions.checkNotNull(obj1);
                checkNotNull(obj1);
                Preconditions.checkNotNull(obj1, "obj1 should not be null");
                Preconditions.checkNotNull(obj1, "%s should not be null", "obj1");
                Preconditions.checkNotNull(obj1.toString());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getVariableUses() {
    writeFile(
        "A.java",
        """
        public class A {
          public String b;

          void foo() {}
        }
        """);
    writeFile(
        "B.java",
        """
        public class B {
          A my;

          B bar() {
            return null;
          }

          void foo(String x, A a) {
            x.trim().intern();
            a.b.trim().intern();
            this.my.foo();
            my.foo();
            this.bar();
            String.valueOf(0);
            java.lang.String.valueOf(1);
            bar().bar();
            System.out.println();
            a.b.indexOf(x.substring(1));
          }
        }
        """);

    TestScanner scanner =
        new TestScanner.Builder()
            .add("x.trim().intern()", "x")
            .add("a.b.trim().intern()", "a")
            .add("this.my.foo()", "this")
            .add("my.foo()", "my")
            .add("this.bar()", "this")
            .add("String.valueOf(0)")
            .add("java.lang.String.valueOf(1)")
            .add("bar().bar()")
            .add("System.out.println()")
            .add("a.b.indexOf(x.substring(1))", "a", "x")
            .build();
    assertCompiles(scanner);
    scanner.assertFoundAll();
  }

  // TODO(mdempsky): Make this more reusable.
  private static class TestScanner extends Scanner {
    private static class Match {
      private final ImmutableList<String> expected;
      private boolean found = false;

      private Match(String... expected) {
        this.expected = ImmutableList.copyOf(expected);
      }
    }

    private static class Builder {
      private final ImmutableMap.Builder<String, Match> builder = ImmutableMap.builder();

      @CanIgnoreReturnValue
      Builder add(String expression, String... expected) {
        builder.put(expression, new Match(expected));
        return this;
      }

      TestScanner build() {
        return new TestScanner(builder.buildOrThrow());
      }
    }

    private final ImmutableMap<String, Match> matches;

    private TestScanner(ImmutableMap<String, Match> matches) {
      this.matches = matches;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, VisitorState state) {
      ExpressionTree expression = node.getExpression();
      Match match = matches.get(expression.toString());
      if (match != null) {
        assertMatch(expression, match.expected);
        match.found = true;
      }
      return super.visitExpressionStatement(node, state);
    }

    private static void assertMatch(ExpressionTree node, List<String> expected) {
      List<IdentifierTree> uses = UnnecessaryCheckNotNull.getVariableUses(node);
      assertWithMessage("variables used in %s", node)
          .that(Lists.transform(uses, Functions.toStringFunction()))
          .isEqualTo(expected);
    }

    void assertFoundAll() {
      for (Map.Entry<String, Match> entry : matches.entrySet()) {
        assertWithMessage("found %s", entry.getKey()).that(entry.getValue().found).isTrue();
      }
    }
  }
}
