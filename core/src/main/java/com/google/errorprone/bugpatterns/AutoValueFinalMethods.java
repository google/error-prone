/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/**
 * Checks the toString(), hashCode() and equals() methods are final in AutoValue classes.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "AutoValueFinalMethods",
    summary =
        "Make toString(), hashCode() and equals() final in AutoValue classes"
            + ", so it is clear to readers that AutoValue is not overriding them",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class AutoValueFinalMethods extends BugChecker implements ClassTreeMatcher {

  private static final String MEMOIZED = "com.google.auto.value.extension.memoized.Memoized";

  // public non-memoized non-final eq/ts/hs methods
  private static final Matcher<MethodTree> METHOD_MATCHER =
      allOf(
          Matchers.<MethodTree>hasModifier(Modifier.PUBLIC),
          not(Matchers.<MethodTree>hasModifier(Modifier.ABSTRACT)),
          not(Matchers.<MethodTree>hasModifier(Modifier.FINAL)),
          not(Matchers.<MethodTree>hasAnnotation(MEMOIZED)),
          anyOf(
              Matchers.equalsMethodDeclaration(),
              Matchers.toStringMethodDeclaration(),
              Matchers.hashCodeMethodDeclaration()));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!ASTHelpers.hasAnnotation(tree, "com.google.auto.value.AutoValue", state)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (Tree memberTree : tree.getMembers()) {
      if (!(memberTree instanceof MethodTree)) {
        continue;
      }
      if (METHOD_MATCHER.matches((MethodTree) memberTree, state)) {
        Optional<SuggestedFix> optionalSuggestedFix =
            SuggestedFixes.addModifiers(memberTree, state, Modifier.FINAL);
        if (optionalSuggestedFix.isPresent()) {
          fix.merge(optionalSuggestedFix.get());
        }
      }
    }
    if (!fix.isEmpty()) {
      return describeMatch(tree, fix.build());
    }
    return NO_MATCH;
  }
}
