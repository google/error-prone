/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.lang.model.element.Modifier;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "UnsynchronizedOverridesSynchronized",
  summary = "Unsynchronized method overrides a synchronized method.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class UnsynchronizedOverridesSynchronized extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);

    if (isSynchronized(methodSymbol)) {
      return Description.NO_MATCH;
    }
    for (MethodSymbol s : ASTHelpers.findSuperMethods(methodSymbol, state.getTypes())) {
      if (isSynchronized(s)) {
        // Input streams are typically not used across threads, so this case isn't
        // worth enforcing.
        if (isSameType(s.owner.type, state.getTypeFromString("java.io.InputStream"), state)) {
          continue;
        }
        return buildDescription(methodTree)
            .addFix(SuggestedFixes.addModifiers(methodTree, state, Modifier.SYNCHRONIZED))
            .setMessage(
                String.format(
                    "Unsynchronized method %s overrides synchronized method in %s",
                    methodSymbol.getSimpleName(), s.enclClass().getSimpleName()))
            .build();
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean isSynchronized(MethodSymbol sym) {
    return sym.getModifiers().contains(Modifier.SYNCHRONIZED);
  }
}
