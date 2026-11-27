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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.TestProcessors.FailingProcessor;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Options;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/// Tests for [BugCheckerRefactoringTestHelper].
@RunWith(JUnit4.class)
public class BugCheckerRefactoringTestHelperTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

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
            public class Bar {}
            """)
        .doTest();
  }

  /// Mock [BugChecker] for testing only.
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

  /// Mock [BugChecker] for testing only.
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
    BugCheckerRefactoringTestHelper.newInstance(SimplifyArrayList.class, getClass())
        .setImportOrder("static-last")
        .addInputLines(
            "pkg/A.java",
            """
            import static java.lang.Math.min;

            class A {
              java.util.ArrayList list;
              int x = min(1, 2);
            }
            """)
        .addOutputLines(
            "out/pkg/A.java",
            """
            import java.util.ArrayList;

            import static java.lang.Math.min;

            class A {
              ArrayList list;
              int x = min(1, 2);
            }
            """)
        .doTest();
  }

  /// Mock [BugChecker] for testing only.
  @BugPattern(
      summary = "Mock refactoring that replaces java.util.ArrayList with ArrayList",
      explanation = "For test purposes only.",
      severity = SUGGESTION)
  public static class SimplifyArrayList extends BugChecker implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      if (state.getSourceForNode(tree.getType()).equals("java.util.ArrayList")) {
        SuggestedFix fix =
            SuggestedFix.builder()
                .replace(tree.getType(), "ArrayList")
                .addImport("java.util.ArrayList")
                .build();
        return describeMatch(tree.getType(), fix);
      }
      return Description.NO_MATCH;
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

  @Test
  public void emptyTestInput() {
    BugCheckerRefactoringTestHelper bugCheckerRefactoringTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(ReplaceVarTypes.class, getClass())
            .addInputLines(
                "Test.java",
                """
                import java.util.List;
                """)
            .expectUnchanged();
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> bugCheckerRefactoringTestHelper.doTest());
    assertThat(thrown)
        .hasMessageThat()
        .contains("Expected a class, package, or module declaration");
  }

  @BugPattern(summary = "Replaces return values if test-only", severity = SUGGESTION)
  public static class TestOnlyRefactoring extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (state.errorProneOptions().isTestOnlyTarget()) {
        return describeMatch(tree, SuggestedFix.replace(tree.getExpression(), "null"));
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void testOnly_falseByDefault() {
    BugCheckerRefactoringTestHelper.newInstance(TestOnlyRefactoring.class, getClass())
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return "hello";
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testOnly_enabled() {
    BugCheckerRefactoringTestHelper.newInstance(TestOnlyRefactoring.class, getClass())
        .setTestOnly()
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return "hello";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return null;
              }
            }
            """)
        .doTest();
  }

  // Also check Option.PROC directly so JDK 23+ test runs (where javac already disables implicit
  // annotation processing by default) still catch regressions affecting JDK 21.
  @BugPattern(summary = "Replaces return values when -proc:none is set", severity = SUGGESTION)
  public static class ProcNoneRefactoring extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (Options.instance(state.context).isSet(Option.PROC, "none")) {
        return describeMatch(tree, SuggestedFix.replace(tree.getExpression(), "null"));
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void implicitAnnotationProcessing_disabledByDefault() {
    BugCheckerRefactoringTestHelper.newInstance(ProcNoneRefactoring.class, getClass())
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return "hello";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void implicitAnnotationProcessing_setArgsOverrideRestoresProcNone() {
    BugCheckerRefactoringTestHelper.newInstance(ProcNoneRefactoring.class, getClass())
        .setArgs("-proc:full")
        .setArgs("-Xlint:all")
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return "hello";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              public Object foo() {
                return null;
              }
            }
            """)
        .doTest();
  }

  @BugPattern(summary = "Adds @Trigger to Target", severity = SUGGESTION)
  public static class AddTriggerRefactoring extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      if (tree.getSimpleName().contentEquals("Target")) {
        return describeMatch(tree, SuggestedFix.prefixWith(tree, "@Trigger\n"));
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void explicitProcessor_runsOnInput() {
    BugCheckerRefactoringTestHelper testHelper =
        BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
            .setArgs(ImmutableList.of("-processor", FailingProcessor.class.getName()))
            .addInputLines("Trigger.java", "public @interface Trigger {}")
            .expectUnchanged()
            .addInputLines(
                "Test.java",
                """
                @Trigger
                public class Test {}
                """)
            .expectUnchanged();
    AssertionError thrown = assertThrows(AssertionError.class, testHelper::doTest);
    assertThat(thrown).hasMessageThat().contains("FailingProcessor ran");
  }

  @Test
  public void explicitProcessor_runsOnRefactoredOutput() {
    BugCheckerRefactoringTestHelper testHelper =
        BugCheckerRefactoringTestHelper.newInstance(AddTriggerRefactoring.class, getClass())
            .setArgs("-processor", FailingProcessor.class.getName())
            .addInputLines("Trigger.java", "public @interface Trigger {}")
            .expectUnchanged()
            .addInputLines("Target.java", "public class Target {}")
            .addOutputLines(
                "Target.java",
                """
                @Trigger
                public class Target {}
                """);
    AssertionError thrown = assertThrows(AssertionError.class, testHelper::doTest);
    assertThat(thrown).hasMessageThat().contains("FailingProcessor ran");
  }

  @Test
  public void serviceLoadedProcessorJar_disabledByDefaultOnBothPasses() throws IOException {
    Path processorJar =
        TestProcessors.createProcessorJar(tempFolder.newFolder().toPath(), FailingProcessor.class);
    BugCheckerRefactoringTestHelper.newInstance(AddTriggerRefactoring.class, getClass())
        .setArgs("-cp", processorJar.toString())
        .addInputLines("Trigger.java", "public @interface Trigger {}")
        .expectUnchanged()
        .addInputLines(
            "AlreadyAnnotated.java",
            """
            @Trigger
            public class AlreadyAnnotated {}
            """)
        .expectUnchanged()
        .addInputLines("Target.java", "public class Target {}")
        .addOutputLines(
            "Target.java",
            """
            @Trigger
            public class Target {}
            """)
        .doTest();
  }

  @Test
  public void serviceLoadedProcessorJar_enabledWithProcFull() throws IOException {
    Path processorJar =
        TestProcessors.createProcessorJar(tempFolder.newFolder().toPath(), FailingProcessor.class);
    BugCheckerRefactoringTestHelper testHelper =
        BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
            .setArgs("-cp", processorJar.toString(), "-proc:full")
            .addInputLines("Trigger.java", "public @interface Trigger {}")
            .expectUnchanged()
            .addInputLines(
                "Test.java",
                """
                @Trigger
                public class Test {}
                """)
            .expectUnchanged();
    AssertionError thrown = assertThrows(AssertionError.class, testHelper::doTest);
    assertThat(thrown).hasMessageThat().contains("FailingProcessor ran");
  }

  @Test
  public void processorPath_doesNotLeakAcrossTests() throws IOException {
    Path processorJar =
        TestProcessors.createProcessorJar(tempFolder.newFolder().toPath(), FailingProcessor.class);
    Path emptyDir = tempFolder.newFolder("empty").toPath();
    Path moduleDir = tempFolder.newFolder("proc-module-path").toPath();

    // First compilation configures ANNOTATION_PROCESSOR_PATH with FailingProcessor.
    BugCheckerRefactoringTestHelper withProcessorPath =
        BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
            .setArgs("--processor-path", processorJar.toString())
            .addInputLines("Trigger.java", "public @interface Trigger {}")
            .expectUnchanged()
            .addInputLines(
                "Test.java",
                """
                @Trigger
                public class Test {}
                """)
            .expectUnchanged();
    AssertionError thrown = assertThrows(AssertionError.class, withProcessorPath::doTest);
    assertThat(thrown).hasMessageThat().contains("FailingProcessor ran");

    // Subsequent -proc:full compilation without --processor-path must not see the leaked JAR.
    BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
        .setArgs("-cp", emptyDir.toString(), "-proc:full")
        .addInputLines("Trigger.java", "public @interface Trigger {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            @Trigger
            public class Test {}
            """)
        .expectUnchanged()
        .doTest();

    // Verify ANNOTATION_PROCESSOR_MODULE_PATH also resets and does not conflict with a subsequent
    // --processor-path compilation.
    BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
        .setArgs("--processor-module-path", moduleDir.toString())
        .addInputLines("Test.java", "public class Test {}")
        .expectUnchanged()
        .doTest();
    BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
        .setArgs("--processor-path", emptyDir.toString())
        .addInputLines("Test.java", "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void danglingSingleArgOption_doesNotConsumeProcNone() {
    BugCheckerRefactoringTestHelper testHelper =
        BugCheckerRefactoringTestHelper.newInstance(ReturnNullRefactoring.class, getClass())
            .setArgs("-cp")
            .addInputLines("Test.java", "public class Test {}")
            .expectUnchanged();
    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, testHelper::doTest);
    assertThat(thrown).hasMessageThat().contains("requires an argument");
  }
}
