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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.Name;

/** @author kayco@google.com (Kayla Walker) & seibelsabrina@google.com (Sabrina Seibel) */
/** Check for variables and types with the same name */
@BugPattern(
    name = "VariableNameSameAsType",
    summary =
        "variableName and type with the same name "
            + "would refer to the static field instead of the class",
    severity = WARNING)
public class VariableNameSameAsType extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree varTree, VisitorState state) {

    Name varName = varTree.getName();
    Matcher<VariableTree> nameSameAsType =
        Matchers.variableType(
            (typeTree, s) -> {
              Symbol typeSymbol = ASTHelpers.getSymbol(typeTree);
              if (typeSymbol != null) {
                return typeSymbol.getSimpleName().contentEquals(varName);
              }
              return false;
            });

    if (!nameSameAsType.matches(varTree, state)) {
      return Description.NO_MATCH;
    }
    String message =
        String.format(
            "Variable named %s has the type %s. Calling methods using \"%s.something\" are "
                + "difficult to distinguish between static and instance methods.",
            varName, varTree.getType(), varName);
    return buildDescription(varTree).setMessage(message).build();
  }
}
