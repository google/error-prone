/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/** Discourages the use of negative boolean names. */
@BugPattern(summary = "Prefer positive boolean names", severity = WARNING)
public final class NegativeBoolean extends BugChecker implements VariableTreeMatcher {

  // Match names beginning with 'no' or 'not'
  private static final Pattern NEGATIVE_NAME = Pattern.compile("^not?[A-Z].*$");

  @Override
  public Description matchVariable(VariableTree node, VisitorState state) {
    // Only consider local variables of type boolean
    Symbol symbol = getSymbol(node);
    if (!symbol.getKind().equals(ElementKind.LOCAL_VARIABLE)) {
      return Description.NO_MATCH;
    }
    if (!symbol.asType().getKind().equals(TypeKind.BOOLEAN)) {
      return Description.NO_MATCH;
    }

    if (isNegativeName(symbol.getSimpleName().toString())) {
      return describeMatch(node);
    }
    return Description.NO_MATCH;
  }

  private static boolean isNegativeName(String name) {
    Matcher m = NEGATIVE_NAME.matcher(name);
    return m.matches();
  }
}
