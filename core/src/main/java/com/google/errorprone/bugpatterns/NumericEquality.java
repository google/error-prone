/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;

/** @author scottjohnson@google.com (Scott Johnson) */
@BugPattern(
  name = "NumericEquality",
  summary = "Numeric comparison using reference equality instead of value equality",
  explanation =
      "Numbers are compared for reference equality/inequality using == or != "
          + "instead of for value equality using .equals()",
  category = JDK,
  severity = ERROR
)
public class NumericEquality extends AbstractReferenceEquality {

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    if (!ASTHelpers.isSubtype(
        ASTHelpers.getType(tree), state.getTypeFromString("java.lang.Number"), state)) {
      return false;
    }
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym instanceof Symbol.VarSymbol && isFinal(sym) && sym.isStatic()) {
      // Using a static final field as a sentinel is OK
      return false;
    }
    return true;
  }

  public static boolean isFinal(Symbol s) {
    return (s.flags() & Flags.FINAL) == Flags.FINAL;
  }
}
