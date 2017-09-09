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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Matches the behaviour of javac's overrides Xlint warning.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "Overrides",
  altNames = "overrides",
  summary = "Varargs doesn't agree for overridden method",
  category = JDK,
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class Overrides extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);
    boolean isVarargs = (methodSymbol.flags() & Flags.VARARGS) != 0;

    Set<MethodSymbol> superMethods = ASTHelpers.findSuperMethods(methodSymbol, state.getTypes());

    // If there are no super methods, we're fine:
    if (superMethods.isEmpty()) {
      return Description.NO_MATCH;
    }

    Iterator<MethodSymbol> superMethodsIterator = superMethods.iterator();
    boolean areSupersVarargs = superMethodsIterator.next().isVarArgs();
    while (superMethodsIterator.hasNext()) {
      if (areSupersVarargs != superMethodsIterator.next().isVarArgs()) {
        // The super methods are inconsistent (some are varargs, some are not varargs). Then the
        // current method is inconsistent with some of its supermethods, so report a match.
        return describeMatch(methodTree);
      }
    }

    // The current method is consistent with all of its supermethods:
    if (isVarargs == areSupersVarargs) {
      return Description.NO_MATCH;
    }

    // The current method is inconsistent with all of its supermethods, so flip the varargs-ness
    // of the current method.

    List<? extends VariableTree> parameterTree = methodTree.getParameters();
    Tree paramType = parameterTree.get(parameterTree.size() - 1).getType();
    CharSequence paramTypeSource = state.getSourceForNode(paramType);
    if (paramTypeSource == null) {
      // No fix if we don't have tree end positions.
      return describeMatch(methodTree);
    }

    Description.Builder descriptionBuilder = buildDescription(methodTree);
    if (isVarargs) {
      descriptionBuilder.addFix(
          SuggestedFix.replace(paramType, "[]", paramTypeSource.length() - 3, 0));
    } else {
      // There may be a comment that includes a '[' character between the open and closed
      // brackets of the array type.  If so, we don't return a fix.
      int arrayOpenIndex = paramTypeSource.length() - 2;
      while (paramTypeSource.charAt(arrayOpenIndex) == ' ') {
        arrayOpenIndex--;
      }
      if (paramTypeSource.charAt(arrayOpenIndex) == '[') {
        descriptionBuilder.addFix(SuggestedFix.replace(paramType, "...", arrayOpenIndex, 0));
      }
    }

    return descriptionBuilder.build();
  }
}
