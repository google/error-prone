/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.formatstring;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.bugpatterns.formatstring.FormatStringValidation.ValidationResult;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Format string validation utility that fails on more cases than {@link FormatStringValidation} to
 * enforce strict format string checking.
 */
public class StrictFormatStringValidation {

  @Nullable
  public static ValidationResult validate(
      ExpressionTree formatStringTree, List<? extends ExpressionTree> args, VisitorState state) {
    String formatStringValue = ASTHelpers.constValue(formatStringTree, String.class);

    // If formatString has a constant value, then it couldn't have been an @FormatString parameter,
    // so don't bother with annotations and just check if the parameters match the format string.
    if (formatStringValue != null) {
      return FormatStringValidation.validate(
          ImmutableList.<ExpressionTree>builder().add(formatStringTree).addAll(args).build(),
          state);
    }

    // TODO(dawasser): Handle effectively final format strings.

    // The format string is not a compile time constant. Check if it is an @FormatString method
    // parameter or is in an @FormatMethod method.
    Symbol formatStringSymbol = ASTHelpers.getSymbol(formatStringTree);
    if (!(formatStringSymbol instanceof VarSymbol)) {
      return ValidationResult.create(
          null,
          String.format(
              "Format strings must be either a literal or a variable. Other expressions"
                  + " are not valid.\n"
                  + "Invalid format string: %s",
              formatStringTree));
    }

    if (formatStringSymbol.getKind() == ElementKind.PARAMETER
        && ASTHelpers.hasAnnotation(formatStringSymbol, FormatString.class, state)) {
      List<VarSymbol> ownerParams = ((MethodSymbol) formatStringSymbol.owner).getParameters();
      int ownerFormatStringIndex = ownerParams.indexOf(formatStringSymbol);

      ImmutableList.Builder<Type> ownerFormatArgTypesBuilder = ImmutableList.builder();
      for (VarSymbol paramSymbol :
          ownerParams.subList(ownerFormatStringIndex + 1, ownerParams.size())) {
        ownerFormatArgTypesBuilder.add(paramSymbol.type);
      }
      ImmutableList<Type> ownerFormatArgTypes = ownerFormatArgTypesBuilder.build();

      Types types = state.getTypes();
      ImmutableList.Builder<Type> calleeFormatArgTypesBuilder = ImmutableList.builder();
      for (ExpressionTree formatArgExpression : args) {
        calleeFormatArgTypesBuilder.add(types.erasure(((JCExpression) formatArgExpression).type));
      }
      ImmutableList<Type> calleeFormatArgTypes = calleeFormatArgTypesBuilder.build();

      if (ownerFormatArgTypes.size() != calleeFormatArgTypes.size()) {
        return ValidationResult.create(
            null,
            String.format(
                "The number of format arguments passed "
                    + "with an @FormatString must match the number of format arguments in the "
                    + "@FormatMethod header where the format string was declared.\n\t"
                    + "Format args passed: %d\n\tFormat args expected: %d",
                calleeFormatArgTypes.size(), ownerFormatArgTypes.size()));
      } else {
        for (int i = 0; i < calleeFormatArgTypes.size(); i++) {
          if (!ASTHelpers.isSameType(
              ownerFormatArgTypes.get(i), calleeFormatArgTypes.get(i), state)) {
            return ValidationResult.create(
                null,
                String.format(
                    "The format argument types passed "
                        + "with an @FormatString must match the types of the format arguments in "
                        + "the @FormatMethod header where the format string was declared.\n\t"
                        + "Format arg types passed: %s\n\tFormat arg types expected: %s",
                    calleeFormatArgTypes.toArray(), ownerFormatArgTypes.toArray()));
          }
        }
      }

      // The arguments matched. Return a successful result.
      return null;
    }

    return ValidationResult.create(
        null,
        String.format(
            "Format strings must be compile time constant or parameters annotated "
                + "@FormatString: %s",
            formatStringTree));
  }

  private StrictFormatStringValidation() {}
}
