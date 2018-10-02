/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.bugpatterns.formatstring.FormatStringValidation.ValidationResult;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Format string validation utility that fails on more cases than {@link FormatStringValidation} to
 * enforce strict format string checking.
 */
public class StrictFormatStringValidation {

  private static final Matcher<ExpressionTree> MOCKITO_ARGUMENT_MATCHER =
      staticMethod().onClass("org.mockito.Matchers");

  @Nullable
  public static ValidationResult validate(
      ExpressionTree formatStringTree, List<? extends ExpressionTree> args, VisitorState state) {
    if (MOCKITO_ARGUMENT_MATCHER.matches(formatStringTree, state)) {
      // Mockito matchers do not pass standard @FormatString requirements, but we allow them so
      // that people can verify @FormatMethod methods.
      return null;
    }

    Stream<String> formatStringValues = FormatStringValidation.constValues(formatStringTree);

    // If formatString has a constant value, then it couldn't have been an @FormatString parameter,
    // so don't bother with annotations and just check if the parameters match the format string.
    if (formatStringValues != null) {
      return FormatStringValidation.validate(
          /* formatMethodSymbol= */ null,
          ImmutableList.<ExpressionTree>builder().add(formatStringTree).addAll(args).build(),
          state);
    }

    // The format string is not a compile time constant. Check if it is an @FormatString method
    // parameter or is in an @FormatMethod method.
    Symbol formatStringSymbol = ASTHelpers.getSymbol(formatStringTree);
    if (!(formatStringSymbol instanceof VarSymbol)) {
      return ValidationResult.create(
          null,
          String.format(
              "Format strings must be either literals or variables. Other expressions"
                  + " are not valid.\n"
                  + "Invalid format string: %s",
              formatStringTree));
    }

    if ((formatStringSymbol.flags() & (Flags.FINAL | Flags.EFFECTIVELY_FINAL)) == 0) {
      return ValidationResult.create(
          null, "All variables passed as @FormatString must be final or effectively final");
    }

    if (formatStringSymbol.getKind() == ElementKind.PARAMETER) {
      return validateFormatStringParameter(formatStringTree, formatStringSymbol, args, state);
    } else {
      // The format string is final but not a method parameter or compile time constant. Ensure that
      // it is only assigned to compile time constant values and ensure that any possible assignment
      // works with the format arguments.
      return validateFormatStringVariable(formatStringTree, formatStringSymbol, args, state);
    }
  }

  /** Helps {@code validate()} validate a format string that is declared as a method parameter. */
  private static ValidationResult validateFormatStringParameter(
      ExpressionTree formatStringTree,
      Symbol formatStringSymbol,
      List<? extends ExpressionTree> args,
      VisitorState state) {
    if (!isFormatStringParameter(formatStringSymbol, state)) {
      return ValidationResult.create(
          null,
          String.format(
              "Format strings must be compile time constants or parameters annotated "
                  + "@FormatString: %s",
              formatStringTree));
    }

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

    // Format string usage was valid.
    return null;
  }

  /**
   * Helps {@code validate()} validate a format string that is a variable, but not a parameter. This
   * method assumes that the format string variable has already been asserted to be final or
   * effectively final.
   */
  private static ValidationResult validateFormatStringVariable(
      ExpressionTree formatStringTree,
      final Symbol formatStringSymbol,
      final List<? extends ExpressionTree> args,
      final VisitorState state) {
    if (formatStringSymbol.getKind() != ElementKind.LOCAL_VARIABLE) {
      return ValidationResult.create(
          null,
          String.format(
              "Variables used as format strings that are not local variables must be compile time"
                  + " constants.\n%s is neither a local variable nor a compile time constant.",
              formatStringTree));
    }

    // Find the Tree for the block in which the variable is defined. If it is not defined in this
    // class (though it may have been in a super class). We require compile time constant values in
    // that case.
    Symbol owner = formatStringSymbol.owner;
    TreePath path = TreePath.getPath(state.getPath(), formatStringTree);
    while (path != null && ASTHelpers.getSymbol(path.getLeaf()) != owner) {
      path = path.getParentPath();
    }

    // A local variable must be declared in a parent tree to be accessed. This case should be
    // impossible.
    if (path == null) {
      throw new IllegalStateException(
          String.format(
              "Could not find the Tree where local variable %s is declared. "
                  + "This should be impossible.",
              formatStringTree));
    }

    // Scan down from the scope where the variable was declared
    ValidationResult result =
        path.getLeaf()
            .accept(
                new TreeScanner<ValidationResult, Void>() {
                  @Override
                  public ValidationResult visitVariable(VariableTree node, Void unused) {
                    if (ASTHelpers.getSymbol(node) == formatStringSymbol) {
                      if (node.getInitializer() == null) {
                        return ValidationResult.create(
                            null,
                            String.format(
                                "Variables used as format strings must be initialized when they are"
                                    + " declared.\nInvalid declaration: %s",
                                node));
                      }
                      return validateStringFromAssignment(node, node.getInitializer(), args, state);
                    }
                    return super.visitVariable(node, unused);
                  }

                  @Override
                  public ValidationResult reduce(ValidationResult r1, ValidationResult r2) {
                    if (r1 == null && r2 == null) {
                      return null;
                    }
                    return MoreObjects.firstNonNull(r1, r2);
                  }
                },
                null);

    return result;
  }

  private static ValidationResult validateStringFromAssignment(
      Tree formatStringAssignment,
      ExpressionTree formatStringRhs,
      List<? extends ExpressionTree> args,
      VisitorState state) {
    String value = ASTHelpers.constValue(formatStringRhs, String.class);
    if (value == null) {
      return ValidationResult.create(
          null,
          String.format(
              "Local format string variables must only be assigned to compile time constant values."
                  + " Invalid format string assignment: %s",
              formatStringAssignment));
    } else {
      return FormatStringValidation.validate(
          /* formatMethodSymbol= */ null,
          ImmutableList.<ExpressionTree>builder().add(formatStringRhs).addAll(args).build(),
          state);
    }
  }

  /**
   * Returns whether an input {@link Symbol} is a format string in a {@link FormatMethod}. This is
   * true if the {@link Symbol} is a {@link String} parameter in a {@link FormatMethod} and is
   * either:
   *
   * <ol>
   *   <li>Annotated with {@link FormatString}
   *   <li>The first {@link String} parameter in the method with no other parameters annotated
   *       {@link FormatString}.
   * </ol>
   */
  private static boolean isFormatStringParameter(Symbol formatString, VisitorState state) {
    Type stringType = state.getSymtab().stringType;

    // The input symbol must be a String and a parameter of a @FormatMethod to be a @FormatString.
    if (!ASTHelpers.isSameType(formatString.type, stringType, state)
        || !(formatString.owner instanceof MethodSymbol)
        || !ASTHelpers.hasAnnotation(formatString.owner, FormatMethod.class, state)) {
      return false;
    }

    // If the format string is annotated @FormatString in a @FormatMethod, it is a format string.
    if (ASTHelpers.hasAnnotation(formatString, FormatString.class, state)) {
      return true;
    }

    // Check if format string is the first string with no @FormatString params in the @FormatMethod.
    MethodSymbol owner = (MethodSymbol) formatString.owner;
    boolean formatStringFound = false;
    for (Symbol param : owner.getParameters()) {
      if (param == formatString) {
        formatStringFound = true;
      }

      if (ASTHelpers.isSameType(param.type, stringType, state)) {
        // If this is a String parameter before the input Symbol, then the input symbol can't be the
        // format string since it wasn't annotated @FormatString.
        if (!formatStringFound) {
          return false;
        } else if (ASTHelpers.hasAnnotation(param, FormatString.class, state)) {
          return false;
        }
      }
    }

    return true;
  }

  private StrictFormatStringValidation() {}
}
