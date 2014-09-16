/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit3TestClass;
import static com.google.errorprone.matchers.JUnitMatchers.wouldRunInJUnit4;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodNameStartsWith;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;

/**
 * @author rburny@google.com (Radoslaw Burny)
 */
@BugPattern(name = "JUnit3TestNotRun",
    summary = "Test method will not be run; please prefix name with \"test\"",
    explanation = "JUnit 3 requires that test method names start with \"test\". The method that" +
        " triggered this error looks like it is supposed to be the test, but either" +
        " misspells the required prefix, or has @Test annotation, but no prefix." +
        " As a consequence, JUnit 3 will ignore it.\n\n" +
        "If you want to disable test on purpose, change the name to something more descriptive," +
        " like \"disabledTestSomething()\". You don't need @Test annotation, but if you want to" +
        " keep it, add @Ignore too.",
    category = JUNIT, maturity = MATURE, severity = ERROR)
public class JUnit3TestNotRun extends BugChecker implements MethodTreeMatcher {

  /*
   * Regular expression for test method name that is misspelled and should be replaced with "test".
   * ".est" and "est"  are omitted, because they catch real words like "restore", "destroy", "best",
   * "establish". ".test" is omitted, because people use it on purpose, to disable the test.
   * Otherwise, I haven't found any false positives; "tes" was most common typo.
   * There are some ambiguities in this regex that lead to bad corrections
   * (i.e. tets -> tests, tesst -> testst), but the error is still found
   * (those could be improved with regex lookahead, but I prefer simpler regex).
   *  TODO(user): see if we can cleanup intentional ".test" misspellings
   */
  private static final Pattern MISSPELLED_NAME = Pattern.compile(
      "t.est|te.st|"    +  // letter inserted
      "tst|tet|tes|"    +  // letter removed
      "etst|tset|tets|" +  // letters swapped
      "t.st|te.t|"      +  // letter changed
      "[tT][eE][sS][tT]"   // miscapitalized
      );

  /**
   * Matches if:
   * 1) Method's name begins with misspelled variation of "test".
   * 2) Method is public, returns void, and has no parameters.
   * 3) Enclosing class is JUnit3 test (extends TestCase, has no RunWith annotation,
   *    and is not abstract).
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    Matcher<MethodTree> methodMatcher = allOf(
        not(methodNameStartsWith("test")),
        Matchers.<MethodTree>hasModifier(Modifier.PUBLIC),
        methodReturns(VOID_TYPE),
        methodHasParameters(),
        enclosingClass(isJUnit3TestClass));
    if (!methodMatcher.matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    String name = methodTree.getName().toString();
    String fixedName;
    // regex.Matcher class name collides with errorprone.Matcher
    java.util.regex.Matcher matcher = MISSPELLED_NAME.matcher(name);
    if (matcher.lookingAt()) {
      fixedName = matcher.replaceFirst("test");
    } else if (wouldRunInJUnit4.matches(methodTree, state)) {
      fixedName = "test" + name.substring(0, 1).toUpperCase() + name.substring(1);
    } else {
      return Description.NO_MATCH;
    }
    // We don't have start position for a method symbol, so we replace everything between result
    // type and body.
    JCMethodDecl decl = (JCMethodDecl) methodTree;
    Fix fix = SuggestedFix.replace(
        decl.restype.getStartPosition() + 4, decl.body.getStartPosition(), " " + fixedName + "() ");
    return describeMatch(methodTree, fix);
  }
}
