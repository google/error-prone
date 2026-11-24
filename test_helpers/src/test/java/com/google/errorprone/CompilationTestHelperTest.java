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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.FatalError;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CompilationTestHelper}. */
@RunWith(JUnit4.class)
public class CompilationTestHelperTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ReturnTreeChecker.class, getClass());

  @Test
  public void fileWithNoBugMarkersAndNoErrorsShouldPass() {
    compilationHelper.addSourceLines("Test.java", "public class Test {}").doTest();
  }

  @Test
  public void fileWithNoBugMarkersAndErrorFails() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                return true;
              }
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Saw unexpected error on line 3");
  }

  @Test
  public void fileWithBugMarkerAndNoErrorsFails() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              // BUG: Diagnostic contains:
              public void doIt() {}
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Did not see an error on line 3");
  }

  @Test
  public void fileWithBugMatcherAndNoErrorsFails() {
    var compilationTestHelper =
        compilationHelper
            .addSourceLines(
                "Test.java",
                """
                public class Test {
                  // BUG: Diagnostic matches: X
                  public void doIt() {}
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern(""));
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Did not see an error on line 3");
  }

  @Test
  public void fileWithBugMarkerAndMatchingErrorSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                // BUG: Diagnostic contains: Method may return normally
                return true;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void fileWithBugMatcherAndMatchingErrorSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                // BUG: Diagnostic matches: X
                return true;
              }
            }
            """)
        .expectErrorMessage("X", Predicates.containsPattern("Method may return normally"))
        .doTest();
  }

  @Test
  public void fileWithBugMarkerAndErrorOnWrongLineFails() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              // BUG: Diagnostic contains:
              public boolean doIt() {
                return true;
              }
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Did not see an error on line 3");
  }

  @Test
  public void fileWithBugMatcherAndErrorOnWrongLineFails() {
    var compilationTestHelper =
        compilationHelper
            .addSourceLines(
                "Test.java",
                """
                public class Test {
                  // BUG: Diagnostic matches: X
                  public boolean doIt() {
                    return true;
                  }
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern(""));
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Did not see an error on line 3");
  }

  @Test
  public void fileWithTwoBugMarkersAndNoErrorsReportsBothLines() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              // BUG: Diagnostic contains: foo
              public void doIt() {}

              // BUG: Diagnostic contains: bar
              public void doItAgain() {}
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo(
            """
            Did not see an error on line 3 matching foo.
            Did not see an error on line 6 matching bar.
            There were no errors.\
            """);
  }

  @Test
  public void fileWithTwoUnexpectedErrorsReportsBothLinesBeforeTheDiagnostics() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                return true;
              }

              public boolean doItAgain() {
                return false;
              }
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            """
            Saw unexpected error on line 3.
            Saw unexpected error on line 7.
            All errors:
            """);
  }

  @Test
  public void markerWithUnknownKeyIsReportedWithTheOtherMismatches() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                return true;
              }

              // BUG: Diagnostic matches: X
              public void doItAgain() {}
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            """
            Saw unexpected error on line 3.
            No expected error message with key [X] as expected from line [6] \
            with diagnostic [// BUG: Diagnostic matches: X]
            All errors:
            """);
  }

  @Test
  public void fileWithUnexpectedErrorBeforeMissingOneReportsBothLines() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                return true;
              }

              // BUG: Diagnostic contains: foo
              public void doItAgain() {}
            }
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            """
            Saw unexpected error on line 3.
            Did not see an error on line 7 matching foo.
            All errors:
            """);
  }

  @Test
  public void mismatchesInTwoFilesAreReportedWithTheirFileNames() {
    var compilationTestHelper =
        compilationHelper
            .addSourceLines(
                "A.java",
                """
                public class A {
                  public boolean doIt() {
                    return true;
                  }
                }
                """)
            .addSourceLines(
                "B.java",
                """
                public class B {
                  // BUG: Diagnostic contains: foo
                  public void doIt() {}
                }
                """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            """
            /A.java: Saw unexpected error on line 3.
            /B.java: Did not see an error on line 3 matching foo.
            All errors:
            """);
  }

  @Test
  public void fileWithMultipleBugMarkersAndMatchingErrorsSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                // BUG: Diagnostic contains: Method may return normally
                return true;
              }

              public String doItAgain() {
                // BUG: Diagnostic contains: Method may return normally
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void fileWithMultipleSameBugMatchersAndMatchingErrorsSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                // BUG: Diagnostic matches: X
                return true;
              }

              public String doItAgain() {
                // BUG: Diagnostic matches: X
                return null;
              }
            }
            """)
        .expectErrorMessage("X", Predicates.containsPattern("Method may return normally"))
        .doTest();
  }

  @Test
  public void fileWithMultipleDifferentBugMatchersAndMatchingErrorsSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                // BUG: Diagnostic matches: X
                return true;
              }

              public String doItAgain() {
                // BUG: Diagnostic matches: Y
                return null;
              }
            }
            """)
        .expectErrorMessage("X", Predicates.containsPattern("Method may return normally"))
        .expectErrorMessage("Y", Predicates.containsPattern("Method may return normally"))
        .doTest();
  }

  @Test
  public void fileWithSyntaxErrorFails() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            // There's a syntax error on this line, but it shouldn't register as an
            // Error Prone diagnostic
            "    return}",
            "}");
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            """
            Did not see an error on line 4 containing [ReturnTreeChecker].
            All errors:
            """);
    assertThat(expected).hasMessageThat().contains("error: illegal start of expression");
  }

  @Test
  public void expectedResultMatchesActualResultSucceeds() {
    compilationHelper
        .expectResult(Result.OK)
        .addSourceLines("Test.java", "public class Test {}")
        .doTest();
  }

  @Test
  public void expectedResultDiffersFromActualResultFails() {
    var compilationTestHelper =
        compilationHelper
            .expectResult(Result.ERROR)
            .addSourceLines("Test.java", "public class Test {}");
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Expected compilation result ERROR, but was OK");
  }

  @Test
  public void fatalCompilerErrorIsReportedAsError() {
    CompilationTestHelper.newInstance(FatalErrorChecker.class, getClass())
        .expectResult(Result.ERROR)
        .expectNoDiagnostics()
        .addSourceLines("Test.java", "class Test {}")
        .doTest();
  }

  @Test
  public void expectNoDiagnoticsAndNoDiagnosticsProducedSucceeds() {
    compilationHelper
        .expectNoDiagnostics()
        .addSourceLines(
            "Test.java",
            """
            // BUG: Diagnostic contains:
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void expectNoDiagnoticsAndNoDiagnosticsProducedSucceedsWithMatches() {
    compilationHelper
        .expectNoDiagnostics()
        .addSourceLines(
            "Test.java",
            """
            // BUG: Diagnostic matches: X
            public class Test {}
            """)
        .expectErrorMessage("X", Predicates.containsPattern(""))
        .doTest();
  }

  @Test
  public void expectNoDiagnoticsButDiagnosticsProducedFails() {
    var compilationTestHelper =
        compilationHelper
            .expectNoDiagnostics()
            .addSourceLines(
                "Test.java",
                """
                public class Test {
                  public boolean doIt() {
                    // BUG: Diagnostic contains:
                    return true;
                  }
                }
                """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Expected no diagnostics produced, but found 1");
  }

  @Test
  public void expectNoDiagnoticsButDiagnosticsProducedFailsWithMatches() {
    var compilationTestHelper =
        compilationHelper
            .expectNoDiagnostics()
            .addSourceLines(
                "Test.java",
                """
                public class Test {
                  public boolean doIt() {
                    // BUG: Diagnostic matches: X
                    return true;
                  }
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern(""));
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Expected no diagnostics produced, but found 1");
  }

  @Test
  public void failureWithErrorAndNoDiagnosticFails() {
    var compilationTestHelper =
        compilationHelper
            .expectNoDiagnostics()
            .addSourceLines("Test.java", "public class Test {}")
            .setArgs(ImmutableList.of("-Xep:ReturnTreeChecker:Squirrels")); // Bad flag crashes.
    InvalidCommandLineOptionException expected =
        assertThrows(InvalidCommandLineOptionException.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .contains("invalid flag: -Xep:ReturnTreeChecker:Squirrels");
  }

  @Test
  public void commandLineArgToDisableCheckWorks() {
    compilationHelper
        .setArgs(ImmutableList.of("-Xep:ReturnTreeChecker:OFF"))
        .expectNoDiagnostics()
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public boolean doIt() {
                // BUG: Diagnostic contains:
                return true;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void missingExpectErrorFails() {
    var compilationTestHelper =
        compilationHelper.addSourceLines(
            "Test.java",
            """
            // BUG: Diagnostic matches: X
            public class Test {}
            """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("No expected error message with key [X]");
  }

  @BugPattern(
      summary = "Method may return normally.",
      explanation = "Consider mutating some global state instead.",
      severity = ERROR)
  public static class ReturnTreeChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void unexpectedDiagnosticOnFirstLine() {
    var compilationTestHelper =
        CompilationTestHelper.newInstance(PackageTreeChecker.class, getClass())
            .addSourceLines(
                "test/Test.java",
                """
                package test;

                public class Test {}
                """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected).hasMessageThat().contains("Package declaration found");
  }

  @BugPattern(
      summary = "Package declaration found",
      explanation = "Prefer to use the default package for everything.",
      severity = ERROR)
  public static class PackageTreeChecker extends BugChecker implements CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      if (tree.getPackage() != null) {
        return describeMatch(tree.getPackage());
      }
      return NO_MATCH;
    }
  }

  /** Test classes used for withClassPath tests */
  public static class WithClassPath extends WithClassPathSuper {}

  public static class WithClassPathSuper {}

  @Test
  public void withClassPath_success() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import " + WithClassPath.class.getCanonicalName() + ";",
            "class Test extends WithClassPath {}")
        .withClasspath(
            CompilationTestHelperTest.class, WithClassPath.class, WithClassPathSuper.class)
        .doTest();
  }

  @Test
  public void withClassPath_failure() {
    // disable checkWellFormed
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import " + WithClassPath.class.getCanonicalName() + ";",
            "// BUG: Diagnostic contains: cannot access "
                + WithClassPathSuper.class.getCanonicalName(),
            "class Test extends WithClassPath {}")
        .withClasspath(CompilationTestHelperTest.class, WithClassPath.class)
        .matchAllDiagnostics()
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void onlyCallDoTestOnce() {
    compilationHelper.addSourceLines("Test.java", "public class Test {}").doTest();
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> compilationHelper.doTest());
    assertThat(expected).hasMessageThat().contains("doTest");
  }

  @Test
  public void assertionErrors_causeTestFailures() {
    var compilationTestHelper =
        CompilationTestHelper.newInstance(AssertionFailingChecker.class, getClass())
            .addSourceLines(
                "test/Test.java",
                """
                package test;

                public class Test {}
                """);
    AssertionError expected =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(expected)
        .hasMessageThat()
        .contains("An unhandled exception was thrown by the Error Prone static analysis plugin");
  }

  // https://github.com/google/error-prone/issues/6178
  @Test
  public void crashIsDetectedRegardlessOfLocale() {
    Locale previousLocale = Locale.getDefault();
    Locale previousDisplayLocale = Locale.getDefault(Locale.Category.DISPLAY);
    Locale previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
    try {
      for (Locale locale :
          ImmutableList.of(
              Locale.US, Locale.GERMANY, Locale.JAPAN, Locale.SIMPLIFIED_CHINESE, Locale.FRANCE)) {
        Locale.setDefault(locale);
        var compilationTestHelper =
            CompilationTestHelper.newInstance(ConstructorFailingChecker.class, getClass())
                .addSourceLines("Test.java", "class Test {}");
        AssertionError expected =
            assertThrows(
                "Locale: " + locale, AssertionError.class, () -> compilationTestHelper.doTest());
        assertWithMessage("Locale: %s", locale)
            .that(expected)
            .hasMessageThat()
            .contains("ErrorProne suffered an internal crash");
        assertWithMessage("Locale: %s", locale)
            .that(expected)
            .hasMessageThat()
            .contains("checker failed to initialize");
        // Make sure a localized crash banner was exercised. javac translates it into Japanese, but
        // some JDKs (e.g. JDK 17) don't translate it into German.
        if (locale.equals(Locale.JAPAN)) {
          assertWithMessage("Locale: %s", locale)
              .that(expected)
              .hasMessageThat()
              .doesNotContain("An exception has occurred in the compiler");
        }
      }
    } finally {
      Locale.setDefault(previousLocale);
      Locale.setDefault(Locale.Category.DISPLAY, previousDisplayLocale);
      Locale.setDefault(Locale.Category.FORMAT, previousFormatLocale);
    }
  }

  /** A BugPattern that throws from its constructor. */
  @BugPattern(summary = "A checker that fails to initialize.", severity = ERROR)
  public static class ConstructorFailingChecker extends BugChecker {
    public ConstructorFailingChecker() {
      throw new IllegalStateException("checker failed to initialize");
    }
  }

  /**
   * A BugPattern that causes javac to return SYSERR. Error Prone only handles {@link Exception}s
   * and {@link AssertionError}s thrown by checks, so the {@link FatalError} reaches javac.
   */
  @BugPattern(summary = "A checker that triggers a fatal compiler error.", severity = ERROR)
  public static class FatalErrorChecker extends BugChecker implements CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      throw new FatalError("fatal compiler error");
    }
  }

  /** A BugPattern that always throws. */
  @BugPattern(summary = "A really broken checker.", severity = ERROR)
  public static class AssertionFailingChecker extends BugChecker
      implements CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      throw new AssertionError();
    }
  }

  @BugPattern(summary = "Replaces var types", severity = ERROR)
  public static class ReplaceVarTypes extends BugChecker implements VariableTreeMatcher {
    @SuppressWarnings("TreeToString")
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Tree type = tree.getType();
      if (ASTHelpers.hasExplicitSource(type, state)) {
        return Description.NO_MATCH;
      }
      return describeMatch(type, SuggestedFix.replace(type, "Object"));
    }
  }

  @SuppressWarnings("MissingTestCall") // used in a method reference in assertThrows
  @Test
  public void replaceVarTypes() {
    // after JDK-8358604 in JDK 27, var types have source positions
    // after JDK-8359383 in JDK 26, var type start and end positions are the same
    assume().that(Runtime.version().feature()).isLessThan(26);
    var compilationTestHelper =
        CompilationTestHelper.newInstance(ReplaceVarTypes.class, getClass())
            .addSourceLines(
                "Test.java",
                """
                public class Test {
                  public void foo() {
                    var x = 1 + 2;
                    System.out.println(x);
                  }
                }
                """);
    AssertionError e = assertThrows(AssertionError.class, compilationTestHelper::doTest);
    assertThat(e)
        .hasMessageThat()
        .contains(
            """
            Test.java:3: error: An unhandled exception was thrown by the Error Prone static analysis plugin.
                var x = 1 + 2;
            """);
    assertThat(e).hasMessageThat().contains("BugPattern: ReplaceVarTypes");
  }

  @BugPattern(summary = "Checks if isTestOnlyTarget is true", severity = ERROR)
  public static class TestOnlyChecker extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      if (state.errorProneOptions().isTestOnlyTarget()) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void testOnly_falseByDefault() {
    CompilationTestHelper.newInstance(TestOnlyChecker.class, getClass())
        .addSourceLines("Test.java", "public class Test {}")
        .doTest();
  }

  @Test
  public void testOnly_enabled() {
    CompilationTestHelper.newInstance(TestOnlyChecker.class, getClass())
        .setTestOnly()
        .addSourceLines(
            "Test.java",
            "// BUG: Diagnostic contains: Checks if isTestOnlyTarget is true",
            "public class Test {}")
        .doTest();
  }
}
