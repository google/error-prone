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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.fixes.SuggestedFixes.removeModifiers;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethod;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit3TestClass;
import static com.google.errorprone.matchers.JUnitMatchers.isJunit3TestCase;
import static com.google.errorprone.matchers.JUnitMatchers.wouldRunInJUnit4;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodNameStartsWith;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;

/** @author rburny@google.com (Radoslaw Burny) */
@BugPattern(
    name = "JUnit3TestNotRun",
    summary =
        "Test method will not be run; please correct method signature "
            + "(Should be public, non-static, and method name should begin with \"test\").",
    category = JUNIT,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class JUnit3TestNotRun extends BugChecker implements MethodTreeMatcher {

  /*
   * Regular expression for test method name that is misspelled and should be replaced with "test".
   * ".est" and "est"  are omitted, because they catch real words like "restore", "destroy", "best",
   * "establish". ".test" is omitted, because people use it on purpose, to disable the test.
   * Otherwise, I haven't found any false positives; "tes" was most common typo.
   * There are some ambiguities in this regex that lead to bad corrections
   * (i.e. tets -> tests, tesst -> testst), but the error is still found
   * (those could be improved with regex lookahead, but I prefer simpler regex).
   *  TODO(rburny): see if we can cleanup intentional ".test" misspellings
   */
  private static final Pattern MISSPELLED_NAME =
      Pattern.compile(
          "t.est|te.st|"
              + // letter inserted
              "tst|tet|tes|"
              + // letter removed
              "etst|tset|tets|"
              + // letters swapped
              "t.st|te.t|"
              + // letter changed
              "[tT][eE][sS][tT]" // miscapitalized
          );

  private static final Matcher<MethodTree> LOOKS_LIKE_TEST_CASE =
      allOf(
          enclosingClass(isJUnit3TestClass),
          not(isJunit3TestCase),
          methodReturns(VOID_TYPE),
          methodHasParameters());

  /**
   * Matches if: 1) Method's name begins with misspelled variation of "test". 2) Method is public,
   * returns void, and has no parameters. 3) Enclosing class is JUnit3 test (extends TestCase, has
   * no {@code @RunWith} annotation, no {@code @Test}-annotated methods, and is not abstract).
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!LOOKS_LIKE_TEST_CASE.matches(methodTree, state)) {
      return NO_MATCH;
    }

    List<SuggestedFix> fixes = new ArrayList<>(0);

    if (not(methodNameStartsWith("test")).matches(methodTree, state)) {
      String fixedName = methodTree.getName().toString();
      // N.B. regex.Matcher class name collides with errorprone.Matcher
      java.util.regex.Matcher matcher = MISSPELLED_NAME.matcher(fixedName);
      if (matcher.lookingAt()) {
        fixedName = matcher.replaceFirst("test");
      } else if (wouldRunInJUnit4.matches(methodTree, state)) {
        fixedName = "test" + fixedName.substring(0, 1).toUpperCase() + fixedName.substring(1);
      } else {
        return NO_MATCH;
      }
      // Rename test method appropriately.
      fixes.add(renameMethod(methodTree, fixedName, state));
    }

    // Make method public (if not already public).
    addModifiers(methodTree, state, Modifier.PUBLIC).ifPresent(fixes::add);
    // Remove any other visibility modifiers (if present).
    removeModifiers(methodTree, state, Modifier.PRIVATE, Modifier.PROTECTED).ifPresent(fixes::add);
    // Remove static modifier (if present).
    // N.B. must occur in separate step because removeModifiers only removes one modifier at a time.
    removeModifiers(methodTree, state, Modifier.STATIC).ifPresent(fixes::add);

    return describeMatch(methodTree, mergeFixes(fixes));
  }

  private static Fix mergeFixes(List<SuggestedFix> fixesToMerge) {
    SuggestedFix.Builder builderForResult = SuggestedFix.builder();
    for (SuggestedFix fix : fixesToMerge) {
      if (fix != null) {
        builderForResult.merge(fix);
      }
    }
    return builderForResult.build();
  }
}
