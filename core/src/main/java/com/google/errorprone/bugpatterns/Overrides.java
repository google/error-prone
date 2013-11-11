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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.BugPattern;
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
import com.sun.tools.javac.tree.JCTree;

/**
 * Matches the behaviour of javac's overrides Xlint warning.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "Overrides", altNames = "overrides",
    summary = "Varargs doesn't agree for overridden method",
    explanation = "A varargs method is overridden by a method with an array parameter, or vice "
        + "versa.  Please match the signature of the method being overridden.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class Overrides extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = (MethodSymbol) ASTHelpers.getSymbol(methodTree);
    boolean isVarargs = (methodSymbol.flags() & Flags.VARARGS) != 0;
    if (ASTHelpers.findSuperMethod(methodSymbol, state.getTypes()) != null
        && isVarargs != superIsVarargs(methodSymbol, state)) {
      List<? extends VariableTree> parameterTree = methodTree.getParameters();
      Tree paramType = parameterTree.get(parameterTree.size() - 1).getType();
      CharSequence paramTypeSource = state.getSourceForNode((JCTree) paramType);
      if (paramTypeSource == null) {
        // No fix if we don't have tree end positions.
        return describeMatch(methodTree, null);
      }

      SuggestedFix fix = null;
      if (isVarargs) {
        fix = new SuggestedFix().replace(paramType, "[]", paramTypeSource.length() - 3, 0);
      } else {
        // There may be a comment that includes a '[' character between the open and closed
        // brackets of the array type.  If so, we don't return a fix.
        int arrayOpenIndex = paramTypeSource.length() - 2;
        while (paramTypeSource.charAt(arrayOpenIndex) == ' ') {
          arrayOpenIndex--;
        }
        if (paramTypeSource.charAt(arrayOpenIndex) == '[') {
          fix = new SuggestedFix().replace(paramType, "...", arrayOpenIndex, 0);
        }
      }

      return describeMatch(methodTree, fix);
    }

    return Description.NO_MATCH;
  }

  public boolean superIsVarargs(MethodSymbol method, VisitorState state) {
    OverridesLookupState lookupCache = OverridesLookupState.instance(state);

    MethodSymbol currentMethod = method;
    Boolean result;
    while (true) {
      result = lookupCache.superIsVarargs.get(currentMethod);
      if (result != null) {
        return result;
      }
      MethodSymbol superMethod = ASTHelpers.findSuperMethod(currentMethod, state.getTypes());
      if (superMethod == null) {
        result = ((currentMethod.flags() & Flags.VARARGS) != 0);
        lookupCache.superIsVarargs.put(currentMethod, result);
        break;
      }

      currentMethod = superMethod;
    }

    return result;
  }

  private static class OverridesLookupState {
    public static OverridesLookupState instance(VisitorState state) {
      OverridesLookupState instance =
          (OverridesLookupState) state.getInstance(OverridesLookupState.class);
      if (instance == null) {
        instance = new OverridesLookupState();
        state.putInstance(OverridesLookupState.class, instance);
      }
      return instance;
    }

    public Map<MethodSymbol, Boolean> superIsVarargs = new HashMap<MethodSymbol, Boolean>();
  }
}
