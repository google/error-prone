/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Comparison using reference equality instead of value equality. Reference equality of"
            + " boxed primitive types is usually not useful, as they are value objects, and it is"
            + " bug-prone, as instances are cached for some values but not others.",
    altNames = {"NumericEquality"},
    severity = ERROR)
public final class BoxedPrimitiveEquality extends AbstractReferenceEquality {

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    var type = getType(tree);
    if (type == null || !isRelevantType(type, state)) {
      return false;
    }

    // Using a static final field as a sentinel is OK
    // TODO(cushon): revisit this assumption carried over from NumericEquality
    return !isStaticConstant(getSymbol(tree));
  }

  private static boolean isRelevantType(Type type, VisitorState state) {
    if (isSubtype(type, JAVA_LANG_NUMBER.get(state), state)) {
      return true;
    }
    switch (state.getTypes().unboxedType(type).getTag()) {
      case BYTE:
      case CHAR:
      case SHORT:
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
        return true;
      default:
        return false;
    }
  }

  private static boolean isStaticConstant(Symbol sym) {
    return sym instanceof VarSymbol && isFinal(sym) && sym.isStatic();
  }

  public static boolean isFinal(Symbol s) {
    return (s.flags() & Flags.FINAL) == Flags.FINAL;
  }

  private static final Supplier<Type> JAVA_LANG_NUMBER =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.Number"));
}
