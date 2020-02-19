/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.containsTestMethod;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestClass;
import static com.google.errorprone.matchers.JUnitMatchers.isJunit3TestCase;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
    name = "JUnit4TestNotRun",
    summary =
        "This looks like a test method but is not run; please add @Test and @Ignore, or, if this"
            + " is a helper method, reduce its visibility.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class JUnit4TestNotRun extends BugChecker implements ClassTreeMatcher {

  private static final String TEST_CLASS = "org.junit.Test";
  private static final String IGNORE_CLASS = "org.junit.Ignore";
  private static final String TEST_ANNOTATION = "@Test ";
  private static final String IGNORE_ANNOTATION = "@Ignore ";

  private static final Matcher<MethodTree> OLD_POSSIBLE_TEST_METHOD =
      allOf(
          hasModifier(PUBLIC),
          methodReturns(VOID_TYPE),
          methodHasParameters(),
          not(JUnitMatchers::hasJUnitAnnotation),
          // Use old logic to determine if this is a JUnit4 test class.
          // Only classes with @RunWith, don't check for other @Test methods.
          not(enclosingClass(hasModifier(Modifier.ABSTRACT))),
          not(enclosingClass(JUnitMatchers.isTestCaseDescendant)),
          enclosingClass(JUnitMatchers.hasJUnit4TestRunner));

  private static final Matcher<MethodTree> POSSIBLE_TEST_METHOD =
      allOf(
          hasModifier(PUBLIC),
          methodReturns(VOID_TYPE),
          methodHasParameters(),
          not(JUnitMatchers::hasJUnitAnnotation));

  private static final Matcher<Tree> NOT_STATIC = not(hasModifier(STATIC));


  private final boolean improvedHeuristic;

  public JUnit4TestNotRun(ErrorProneFlags flags) {
    improvedHeuristic = flags.getBoolean("JUnit4TestNotRun:DoNotRequireRunWith").orElse(true);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (improvedHeuristic && !isJUnit4TestClass.matches(tree, state)) {
      return NO_MATCH;
    }
    Matcher<MethodTree> matcher =
        improvedHeuristic ? POSSIBLE_TEST_METHOD : OLD_POSSIBLE_TEST_METHOD;
    Map<MethodSymbol, MethodTree> suspiciousMethods = new HashMap<>();
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodTree methodTree = (MethodTree) member;
      if (matcher.matches(methodTree, state) && !isSuppressed(tree)) {
        suspiciousMethods.put(getSymbol(methodTree), methodTree);
      }
    }
    if (suspiciousMethods.isEmpty()) {
      return NO_MATCH;
    }
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(
              MethodInvocationTree methodInvocationTree, Void unused) {
            suspiciousMethods.remove(getSymbol(methodInvocationTree));
            return super.visitMethodInvocation(methodInvocationTree, null);
          }
        },
        null);

    for (MethodTree methodTree : suspiciousMethods.values()) {
      handleMethod(methodTree, state).ifPresent(state::reportMatch);
    }
    return NO_MATCH;
  }

  /**
   * Matches if:
   *
   * <ol>
   *   <li>The method is public, void, and has no parameters;
   *   <li>the method is not already annotated with {@code @Test}, {@code @Before}, {@code @After},
   *       {@code @BeforeClass}, or {@code @AfterClass};
   *   <li>the enclosing class appears to be intended to run in JUnit4, that is:
   *       <ol type="a">
   *         <li>it is non-abstract,
   *         <li>it does not extend JUnit3 {@code TestCase},
   *         <li>it has an {@code @RunWith} annotation or at least one other method annotated
   *             {@code @Test};
   *       </ol>
   *   <li>and, the method appears to be a test method, that is:
   *       <ol type="a">
   *         <li>or, the method body contains a method call with a name that contains "assert",
   *             "verify", "check", "fail", or "expect".
   *       </ol>
   * </ol>
   */
  private Optional<Description> handleMethod(MethodTree methodTree, VisitorState state) {
    // Method appears to be a JUnit 3 test case (name prefixed with "test"), probably a test.
    if (isJunit3TestCase.matches(methodTree, state)) {
      return Optional.of(describeFixes(methodTree, state));
    }


    // Method is annotated, probably not a test.
    List<? extends AnnotationTree> annotations = methodTree.getModifiers().getAnnotations();
    if (annotations != null && !annotations.isEmpty()) {
      return Optional.empty();
    }

    // Method non-static and contains call(s) to testing method, probably a test,
    // unless it is called elsewhere in the class, in which case it is a helper method.
    if (NOT_STATIC.matches(methodTree, state) && containsTestMethod(methodTree)) {
      return Optional.of(describeFixes(methodTree, state));
    }
    return Optional.empty();
  }

  /**
   * Returns a finding for the given method tree containing fixes:
   *
   * <ol>
   *   <li>Add @Test, remove static modifier if present.
   *   <li>Add @Test and @Ignore, remove static modifier if present.
   *   <li>Change visibility to private (for local helper methods).
   * </ol>
   */
  private Description describeFixes(MethodTree methodTree, VisitorState state) {
    Optional<SuggestedFix> removeStatic =
        SuggestedFixes.removeModifiers(methodTree, state, Modifier.STATIC);
    SuggestedFix testFix =
        SuggestedFix.builder()
            .merge(removeStatic.orElse(null))
            .addImport(TEST_CLASS)
            .prefixWith(methodTree, TEST_ANNOTATION)
            .build();
    SuggestedFix ignoreFix =
        SuggestedFix.builder()
            .merge(testFix)
            .addImport(IGNORE_CLASS)
            .prefixWith(methodTree, IGNORE_ANNOTATION)
            .build();

    SuggestedFix visibilityFix =
        SuggestedFix.builder()
            .merge(SuggestedFixes.removeModifiers(methodTree, state, Modifier.PUBLIC).orElse(null))
            .merge(SuggestedFixes.addModifiers(methodTree, state, Modifier.PRIVATE).orElse(null))
            .build();

    // Suggest @Ignore first if test method is named like a purposely disabled test.
    String methodName = methodTree.getName().toString();
    if (methodName.startsWith("disabl") || methodName.startsWith("ignor")) {
      return buildDescription(methodTree)
          .addFix(ignoreFix)
          .addFix(testFix)
          .addFix(visibilityFix)
          .build();
    }
    return buildDescription(methodTree)
        .addFix(testFix)
        .addFix(ignoreFix)
        .addFix(visibilityFix)
        .build();
  }
}
