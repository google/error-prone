/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link BugCheckerRefactoringTestHelper}. */
@RunWith(JUnit4.class)
public class BugCheckerRefactoringTestHelperTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass());

  @Test
  public void noMatch() {
    helper
        .addInputLines("in/Test.java", "public class Test {}")
        .addOutputLines("out/Test.java", "public class Test {}")
        .doTest();
  }

  @Test
  public void replace() {
    helper
        .addInputLines(
            "in/Test.java",
            """
            public class Test {
              public Object foo() {
                Integer i = 1 + 2;
                return i;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            public class Test {
              public Object foo() {
                Integer i = 1 + 2;
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void replaceFail() {
    assertThrows(
        AssertionError.class,
        () ->
            helper
                .addInputLines(
                    "in/Test.java",
                    """
                    public class Test {
                      public Object foo() {
                        Integer i = 1 + 2;
                        return i;
                      }
                    }
                    """)
                .addOutputLines(
                    "out/Test.java",
                    """
                    public class Test {
                      public Object foo() {
                        Integer i = 1 + 2;
                        return i;
                      }
                    }
                    """)
                .doTest());
  }

  @Test
  public void replaceTextMatch() {
    helper
        .addInputLines(
            "in/Test.java",
            """
            public class Test {
              public Object foo() {
                Integer i = 1 + 2;
                return i;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            public class Test {
              public Object foo() {
                Integer i = 1 + 2;
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ignoreWhitespace() {
    helper
        .addInputLines(
            "in/Test.java",
            """
            public class Test {
              public Object foo() {
                Integer i = 2 + 1;
                return i;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            public class Test {
              public Object foo() {
                Integer i = 2 + 1;
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void replaceTextMatchFail() {
    assertThrows(
        AssertionError.class,
        () ->
            helper
                .addInputLines(
                    "in/Test.java",
                    """
                    public class Test {
                      public Object foo() {
                        Integer i = 1 + 2;
                        return i;
                      }
                    }
                    """)
                .addOutputLines(
                    "out/Test.java",
                    """
                    public class Test {
                      public Object foo() {
                        Integer i = 2 + 1;
                        return null;
                      }
                    }
                    """)
                .doTest());
  }

  @Test
  public void compilationErrorFail() {
    try {
      helper
          .addInputLines("syntax_error.java", "public clazz Bar { ! this should fail }")
          .expectUnchanged()
          .doTest();
    } catch (AssertionError e) {
      assertThat(e).hasMessageThat().contains("compilation failed unexpectedly");
      return;
    }
    fail("compilation succeeded unexpectedly");
  }

  @Test
  public void annotationFullName() {
    BugCheckerRefactoringTestHelper.newInstance(RemoveAnnotationRefactoring.class, getClass())
        .addInputLines(
            "bar/Foo.java",
            """
            package bar;

            public @interface Foo {};
            """)
        .expectUnchanged()
        .addInputLines(
            "foo/Bar.java",
            """
            import bar.Foo;

            public @Foo class Bar {}
            """)
        .addOutputLines(
            "out/foo/Bar.java",
            """
            import bar.Foo;

            public class Bar {}
            """)
        .doTest();
  }

  /** Mock {@link BugChecker} for testing only. */
  @BugPattern(
      summary = "Mock refactoring that replaces all returns with 'return null;' statement.",
      explanation = "For test purposes only.",
      severity = SUGGESTION)
  public static class ReturnNullRefactoring extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree, SuggestedFix.replace(tree, "return null;"));
    }
  }

  /** Mock {@link BugChecker} for testing only. */
  @BugPattern(
      summary = "Mock refactoring that removes all annotations declared in package bar ",
      explanation = "For test purposes only.",
      severity = SUGGESTION)
  public static class RemoveAnnotationRefactoring extends BugChecker
      implements AnnotationTreeMatcher {

    @Override
    public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
      if (ASTHelpers.getType(tree).asElement().toString().startsWith("bar.")) {
        return describeMatch(tree, SuggestedFix.replace(tree, ""));
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void compilationError() {
    try {
      helper
          .addInputLines("Test.java", "public class Test extends NoSuch {}")
          .expectUnchanged()
          .doTest();
    } catch (AssertionError e) {
      assertThat(e).hasMessageThat().contains("error: cannot find symbol");
      return;
    }
    fail("compilation succeeded unexpectedly");
  }

  @Test
  public void staticLastImportOrder() {
    BugCheckerRefactoringTestHelper.newInstance(ImportArrayList.class, getClass())
        .setImportOrder("static-last")
        .addInputLines(
            "pkg/A.java",
            """
            import static java.lang.Math.min;

            class A {}
            """)
        .addOutputLines(
            "out/pkg/A.java",
            """
            import static java.lang.Math.min;

            import java.util.ArrayList;

            class A {}
            """)
        .doTest();
  }

  /** Mock {@link BugChecker} for testing only. */
  @BugPattern(
      summary = "Mock refactoring that imports an ArrayList",
      explanation = "For test purposes only.",
      severity = SUGGESTION)
  public static class ImportArrayList extends BugChecker implements CompilationUnitTreeMatcher {

    @Override
    public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      SuggestedFix fix = SuggestedFix.builder().addImport("java.util.ArrayList").build();
      return describeMatch(tree, fix);
    }
  }

  @Test
  public void onlyCallDoTestOnce() {
    helper.addInputLines("Test.java", "public class Test {}").expectUnchanged().doTest();
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> helper.doTest());
    assertThat(expected).hasMessageThat().contains("doTest");
  }

  @BugPattern(summary = "Replaces var types", severity = SUGGESTION)
  public static class ReplaceVarTypes extends BugChecker implements VariableTreeMatcher {
    @SuppressWarnings("TreeToString")
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Tree type = tree.getType();
      return describeMatch(type, SuggestedFix.replace(type, "Object"));
    }
  }

  @SuppressWarnings("MissingTestCall") // used in a method reference in assertThrows
  @Test
  public void replaceVarTypes() {
    // after JDK-8358604 in JDK 27, var types have source positions
    // after JDK-8359383 in JDK 26, var type start and end positions are the same
    assume().that(Runtime.version().feature()).isLessThan(26);
    BugCheckerRefactoringTestHelper helper =
        BugCheckerRefactoringTestHelper.newInstance(ReplaceVarTypes.class, getClass())
            .addInputLines(
                "Test.java",
                """
                public class Test {
                  public void foo() {
                    var x = 1 + 2;
                    System.out.println(x);
                  }
                }
                """)
            .expectUnchanged();
    AssertionError e = assertThrows(AssertionError.class, helper::doTest);
    assertThat(e)
        .hasMessageThat()
        .contains(
            """
            Test.java:3: error: An unhandled exception was thrown by the Error Prone static analysis plugin.
                var x = 1 + 2;
            """);
    assertThat(e).hasMessageThat().contains("BugPattern: ReplaceVarTypes");
  }

  @Test
  public void varTypeSourcePositions() {
    // after JDK-8358604 in JDK 27, var types have source positions
    assume().that(Runtime.version().feature()).isAtLeast(27);
    BugCheckerRefactoringTestHelper.newInstance(ReplaceVarTypes.class, getClass())
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public void foo() {
                var x = 1 + 2;
                System.out.println(x);
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            public class Test {
              public void foo() {
                Object x = 1 + 2;
                System.out.println(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void reorderImports() {
    helper
        .addInputLines(
            "in/Test.java",
            """
            import static java.lang.Math.min;

            import java.util.List;

            public class Test {
              public Integer foo(List<Integer> xs) {
                Integer i = null;
                for (int x : xs) {
                  if (i == null) {
                    i = x;
                  } else {
                    i = min(i, x);
                  }
                }
                return i;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import static java.lang.Math.min;

            import java.util.List;

            public class Test {
              public Integer foo(List<Integer> xs) {
                Integer i = null;
                for (int x : xs) {
                  if (i == null) {
                    i = x;
                  } else {
                    i = min(i, x);
                  }
                }
                return null;
              }
            }
            """)
        .doTest();
  }
}
