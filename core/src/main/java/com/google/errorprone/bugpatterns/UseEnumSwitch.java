/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "UseEnumSwitch",
    summary = "Prefer using a switch instead of a chained if-else for enums",
    severity = SUGGESTION)
public class UseEnumSwitch extends AbstractUseSwitch {

  @Override
  protected @Nullable String getExpressionForCase(VisitorState state, ExpressionTree argument) {
    Symbol sym = ASTHelpers.getSymbol(argument);
    return sym != null && sym.getKind().equals(ElementKind.ENUM_CONSTANT)
        ? sym.getSimpleName().toString()
        : null;
  }
}
