/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BugCheckerTest {
  @Test
  public void isSuppressed_withoutVisitorState() {
    CompilationTestHelper.newInstance(LegacySuppressionCheck.class, getClass())
        .addSourceLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    // BUG: Diagnostic contains: []",
            "    int unsuppressed;",
            "    // BUG: Diagnostic contains: []",
            "    @SuppressWarnings(\"foo\") int unrelatedSuppression;",
            "    // BUG: Diagnostic contains: [Suppressible, SuppressibleTps, ManualIsSuppressed]",
            "    @SuppressWarnings(\"Suppressible\") int suppressed;",
            "    // BUG: Diagnostic contains: [Suppressible, SuppressibleTps, ManualIsSuppressed]",
            "    @SuppressWarnings(\"Alternative\") int suppressedWithAlternativeName;",
            "    // BUG: Diagnostic contains: [Suppressible, SuppressibleTps, ManualIsSuppressed]",
            "    @SuppressWarnings(\"all\") int allSuppressed;",
            "    // BUG: Diagnostic contains: [Suppressible, SuppressibleTps, ManualIsSuppressed]",
            "    @SuppressWarnings({\"foo\", \"Suppressible\"}) int alsoSuppressed;",
            "    // BUG: Diagnostic contains: [Suppressible, SuppressibleTps, ManualIsSuppressed]",
            "    @SuppressWarnings({\"all\", \"foo\"}) int redundantlySuppressed;",
            "    // BUG: Diagnostic contains: [Suppressible, SuppressibleTps, ManualIsSuppressed]",
            "    @SuppressWarnings({\"all\", \"OnlySuppressedInsideDeprecatedCode\"}) int"
                + " ineffectiveSuppression;",
            "    // BUG: Diagnostic contains: []",
            "    @Deprecated int unuspportedSuppression;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isSuppressed() {
    CompilationTestHelper.newInstance(SuppressibleCheck.class, getClass())
        .addSourceLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    int unsuppressed;",
            "    // BUG: Diagnostic contains:",
            "    @SuppressWarnings(\"foo\") int unrelatedSuppression;",
            "    @SuppressWarnings(\"Suppressible\") int suppressed;",
            "    @SuppressWarnings(\"Alternative\") int suppressedWithAlternativeName;",
            "    @SuppressWarnings(\"all\") int allSuppressed;",
            "    @SuppressWarnings({\"foo\", \"Suppressible\"}) int alsoSuppressed;",
            "    @SuppressWarnings({\"all\", \"foo\"}) int redundantlySuppressed;",
            "    System.out.println(s(() -> {",
            "      // BUG: Diagnostic contains: ",
            "      int insideCalToMethodWhoseDeclarationHasASuppression;",
            "    }));",
            "  }",
            "  @SuppressWarnings(\"all\")",
            "  String s(Runnable r) {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isSuppressed_customSuppressionAnnotation() {
    CompilationTestHelper.newInstance(CustomSuppressibilityCheck.class, getClass())
        .addSourceLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    int unsuppressed;",
            "    // BUG: Diagnostic contains:",
            "    @SuppressWarnings({\"all\", \"OnlySuppressedInsideDeprecatedCode\"}) int"
                + " ineffectiveSuppression;",
            "    @Deprecated int suppressed;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isSuppressed_suppressibleTreePathScanner() {
    CompilationTestHelper.newInstance(SuppressibleTreePathScannerCheck.class, getClass())
        .addSourceLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    int unsuppressed;",
            "    // BUG: Diagnostic contains:",
            "    @SuppressWarnings(\"foo\") int unrelatedSuppression;",
            "    @SuppressWarnings(\"Suppressible\") int suppressed;",
            "    @SuppressWarnings(\"Alternative\") int suppressedWithAlternativeName;",
            "    @SuppressWarnings(\"all\") int allSuppressed;",
            "    @SuppressWarnings({\"foo\", \"Suppressible\"}) int alsoSuppressed;",
            "    @SuppressWarnings({\"all\", \"foo\"}) int redundantlySuppressed;",
            "    System.out.println(s(() -> {",
            "      // BUG: Diagnostic contains: ",
            "      int insideCalToMethodWhoseDeclarationHasASuppression;",
            "    }));",
            "  }",
            "  @SuppressWarnings(\"all\")",
            "  String s(Runnable r) {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isSuppressed_manualIsSuppressed() {
    CompilationTestHelper.newInstance(ManuallySuppressibleCheck.class, getClass())
        .addSourceLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    int unsuppressed;",
            "    // BUG: Diagnostic contains:",
            "    @SuppressWarnings(\"foo\") int unrelatedSuppression;",
            "    @SuppressWarnings(\"Suppressible\") int suppressed;",
            "    @SuppressWarnings(\"Alternative\") int suppressedWithAlternativeName;",
            "    @SuppressWarnings(\"all\") int allSuppressed;",
            "    @SuppressWarnings({\"foo\", \"Suppressible\"}) int alsoSuppressed;",
            "    @SuppressWarnings({\"all\", \"foo\"}) int redundantlySuppressed;",
            "    System.out.println(s(() -> {",
            "      // BUG: Diagnostic contains: ",
            "      int insideCalToMethodWhoseDeclarationHasASuppression;",
            "    }));",
            "  }",
            "  @SuppressWarnings(\"all\")",
            "  String s(Runnable r) {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isSuppressed_disableWarningsInGeneratedCode() {
    CompilationTestHelper.newInstance(CustomSuppressibilityCheck.class, getClass())
        .setArgs("-XepDisableWarningsInGeneratedCode")
        .addSourceLines(
            "A.java",
            "import javax.annotation.processing.Generated;",
            "class A {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    @Generated(\"some-tool\") int unsuppressed;",
            "  }",
            "}")
        .doTest();

    // The check is suppressed if its severity is downgraded to `WARNING`.
    CompilationTestHelper.newInstance(CustomSuppressibilityCheck.class, getClass())
        .setArgs(
            "-XepDisableWarningsInGeneratedCode", "-Xep:OnlySuppressedInsideDeprecatedCode:WARN")
        .addSourceLines(
            "A.java",
            "import javax.annotation.processing.Generated;",
            "class A {",
            "  void m() {",
            "    @Generated(\"some-tool\") int unsuppressed;",
            "  }",
            "}")
        .doTest();
  }

  @BugPattern(
      name = "SuppressionReporter",
      summary =
          "Tells whether some other checks are suppressed according to the deprecated method "
              + "`BugChecker#isSuppressed(Tree)`",
      severity = ERROR,
      suppressionAnnotations = {})
  public static final class LegacySuppressionCheck extends BugChecker
      implements VariableTreeMatcher {
    private final ImmutableList<BugChecker> checks =
        ImmutableList.of(
            new SuppressibleCheck(),
            new CustomSuppressibilityCheck(),
            new SuppressibleTreePathScannerCheck(),
            new ManuallySuppressibleCheck());

    @Override
    @SuppressWarnings("deprecation") // testing deprecated method
    public Description matchVariable(VariableTree tree, VisitorState state) {
      ImmutableList<String> suppressions =
          checks.stream()
              .filter(check -> check.isSuppressed(tree))
              .map(BugChecker::canonicalName)
              .collect(toImmutableList());

      return buildDescription(tree).setMessage("Suppressions: " + suppressions).build();
    }
  }

  @BugPattern(
      name = "Suppressible",
      altNames = "Alternative",
      summary = "Can be suppressed",
      severity = ERROR)
  public static final class SuppressibleCheck extends BugChecker implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(
      name = "OnlySuppressedInsideDeprecatedCode",
      summary = "Can be suppressed using `@Deprecated`, but not `@SuppressWarnings`",
      severity = ERROR,
      suppressionAnnotations = Deprecated.class)
  public static final class CustomSuppressibilityCheck extends BugChecker
      implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(
      name = "SuppressibleTps",
      altNames = {"Suppressible", "Alternative"},
      summary =
          "Should be suppressible with `@SuppressWarnings` but is implemented with"
              + " SuppressibleTreePathScanner",
      severity = ERROR)
  public static final class SuppressibleTreePathScannerCheck extends BugChecker
      implements CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(
        CompilationUnitTree tree, VisitorState stateForCompilationUnit) {
      new SuppressibleTreePathScanner<Void, Void>(stateForCompilationUnit) {
        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
          state().reportMatch(describeMatch(tree));
          return null;
        }

        private VisitorState state() {
          return stateForCompilationUnit.withPath(getCurrentPath());
        }
      }.scan(tree, null);
      return NO_MATCH;
    }
  }

  @BugPattern(
      name = "ManualIsSuppressed",
      altNames = {"Suppressible", "Alternative"},
      summary =
          "Should be suppressible with `@SuppressWarnings` but is implemented with manual"
              + " isSuppressed checks",
      severity = ERROR)
  public static final class ManuallySuppressibleCheck extends BugChecker
      implements CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(
        CompilationUnitTree compilationUnit, VisitorState stateForCompilationUnit) {
      new TreePathScanner<Void, Void>() {
        @Override
        public Void scan(Tree tree, Void unused) {
          if (isSuppressed(tree, state())) {
            return null;
          }
          return super.scan(tree, unused);
        }

        @Override
        public Void scan(TreePath path, Void unused) {
          if (isSuppressed(path.getLeaf(), stateForCompilationUnit.withPath(path))) {
            return null;
          }
          return super.scan(path, unused);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
          state().reportMatch(describeMatch(tree));
          return null;
        }

        private VisitorState state() {
          return stateForCompilationUnit.withPath(getCurrentPath());
        }
      }.scan(stateForCompilationUnit.getPath(), null);
      return NO_MATCH;
    }
  }
}
