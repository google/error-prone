/*
 * Copyright 2017 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class ProvidesFixCheckerTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ProvidesFixChecker(), getClass());
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ProvidesFixChecker.class, getClass());

  @Test
  public void noFixes() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(name = \"Example\", summary = \"\", severity = SeverityLevel.ERROR)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return Description.NO_MATCH;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findingOnAnnotation() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "// BUG: Diagnostic contains: ProvidesFix",
            "@BugPattern(name = \"Example\", summary = \"\", severity = SeverityLevel.ERROR)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree tree, VisitorState s) {",
            "    Description desc =",
            "        buildDescription(tree).addFix(SuggestedFix.delete(tree)).build();",
            "    return desc;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void addsArgument() {
    refactoringTestHelper
        .addInputLines(
            "in/ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.ProvidesFix;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(name = \"Example\", summary = \"\", severity = SeverityLevel.ERROR)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return describeMatch(t, SuggestedFix.replace(t, \"goodbye\"));",
            "  }",
            "}")
        .addOutputLines(
            "out/ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.ProvidesFix;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(",
            "    name = \"Example\",",
            "    summary = \"\",",
            "    severity = SeverityLevel.ERROR,",
            "    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return describeMatch(t, SuggestedFix.replace(t, \"goodbye\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void replacesExistingInPlace() {
    refactoringTestHelper
        .addInputLines(
            "in/ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.ProvidesFix;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(",
            "    name = \"Example\",",
            "    summary = \"\",",
            "    providesFix = ProvidesFix.NO_FIX,",
            "    severity = SeverityLevel.ERROR)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return describeMatch(t, SuggestedFix.replace(t, \"goodbye\"));",
            "  }",
            "}")
        .addOutputLines(
            "out/ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.ProvidesFix;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(",
            "    name = \"Example\",",
            "    summary = \"\",",
            "    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION,",
            "    severity = SeverityLevel.ERROR)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return describeMatch(t, SuggestedFix.replace(t, \"goodbye\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void alreadyHasFixTag() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.ProvidesFix;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(",
            "    name = \"Example\",",
            "    summary = \"\",",
            "    severity = SeverityLevel.ERROR,",
            "    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return describeMatch(t, SuggestedFix.replace(t, \"goodbye\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void erroneousFixTag() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.ProvidesFix;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "@BugPattern(",
            "    name = \"Example\",",
            "    summary = \"\",",
            "    severity = SeverityLevel.ERROR,",
            "    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree t, VisitorState s) {",
            "    return Description.NO_MATCH;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedNewClass() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.BugPattern.SeverityLevel;",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import com.google.errorprone.fixes.SuggestedFix;",
            "import com.google.errorprone.matchers.Description;",
            "import com.sun.source.tree.ClassTree;",
            "// BUG: Diagnostic contains: ProvidesFix",
            "@BugPattern(name = \"Example\", summary = \"\", severity = SeverityLevel.ERROR)",
            "public class ExampleChecker extends BugChecker implements ClassTreeMatcher {",
            "  @Override public Description matchClass(ClassTree tree, VisitorState s) {",
            "    Helper wrapper = new Helper(new Description(",
            "        tree, \"\", SuggestedFix.builder().build(), SeverityLevel.ERROR));",
            "    return wrapper.d;",
            "  }",
            "  static class Helper {",
            "    public Description d;",
            "    public Helper(Description d) { this.d = d; }",
            "  }",
            "}")
        .doTest();
  }
}
