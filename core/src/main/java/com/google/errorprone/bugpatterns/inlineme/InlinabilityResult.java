/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inlineme;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isLocal;
import static com.google.errorprone.util.ASTHelpers.isSuper;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;

/** Whether an API can have {@code @InlineMe} applied to it or not. */
@AutoValue
abstract class InlinabilityResult {

  abstract @Nullable InlineValidationErrorReason error();

  abstract @Nullable ExpressionTree body();

  abstract @Nullable String additionalErrorInfo();

  final String errorMessage() {
    checkState(error() != null);
    String message = error().getErrorMessage();
    if (additionalErrorInfo() != null) {
      message += " " + additionalErrorInfo();
    }
    return message;
  }

  static InlinabilityResult fromError(InlineValidationErrorReason errorReason) {
    return fromError(errorReason, null);
  }

  static InlinabilityResult fromError(
      InlineValidationErrorReason errorReason, ExpressionTree body) {
    return fromError(errorReason, body, null);
  }

  static InlinabilityResult fromError(
      InlineValidationErrorReason errorReason, ExpressionTree body, String additionalErrorInfo) {
    return new AutoValue_InlinabilityResult(errorReason, body, additionalErrorInfo);
  }

  static InlinabilityResult inlinable(ExpressionTree body) {
    return new AutoValue_InlinabilityResult(null, body, null);
  }

  boolean isValidForSuggester() {
    return isValidForValidator()
        || error() == InlineValidationErrorReason.METHOD_CAN_BE_OVERRIDDEN_BUT_CAN_BE_FIXED;
  }

  boolean isValidForValidator() {
    return error() == null;
  }

  enum InlineValidationErrorReason {
    NO_BODY("InlineMe cannot be applied to abstract methods."),
    NOT_EXACTLY_ONE_STATEMENT("InlineMe cannot inline methods with more than 1 statement."),
    COMPLEX_STATEMENT(
        "InlineMe cannot inline complex statements. Consider using a different refactoring tool"),
    CALLS_DEPRECATED_OR_PRIVATE_APIS(
        "InlineMe cannot be applied when the implementation references deprecated or less visible"
            + " API elements:"),
    API_IS_PRIVATE("InlineMe cannot be applied to private APIs."),
    BODY_WOULD_EVALUATE_DIFFERENTLY(
        "Inlining this method will result in a change in evaluation timing for one or more"
            + " arguments to this method."),
    METHOD_CAN_BE_OVERRIDDEN_AND_CANT_BE_FIXED(
        "Methods that are inlined should not be overridable, as the implementation of an overriding"
            + " method may be different than the inlining"),

    // Technically an error in the case where an existing @InlineMe annotation is applied, but could
    // be fixed while suggesting
    METHOD_CAN_BE_OVERRIDDEN_BUT_CAN_BE_FIXED(
        "Methods that are inlined should not be overridable, as the implementation of an overriding"
            + " method may be different than the inlining"),
    VARARGS_USED_UNSAFELY(
        "When using a varargs parameter, it must only be passed in the last position of a method"
            + " call to another varargs method"),
    EMPTY_VOID("InlineMe cannot yet be applied to no-op void methods"),
    REUSE_OF_ARGUMENTS("Implementations cannot use an argument more than once:"),
    PARAM_NAMES_ARE_NAMED_ARGN("Method parameter names cannot match `arg[0-9]+`."),
    NO_SUPER_CALLS("super(...) calls cannot be inlined.");

    private final @Nullable String errorMessage;

    InlineValidationErrorReason(@Nullable String errorMessage) {
      this.errorMessage = errorMessage;
    }

    String getErrorMessage() {
      return errorMessage;
    }
  }

  static boolean matchesArgN(String paramName) {
    return paramName.matches("arg[0-9]+");
  }

  static InlinabilityResult forMethod(MethodTree methodTree, VisitorState state) {
    if (methodTree.getBody() == null) {
      return fromError(InlineValidationErrorReason.NO_BODY);
    }

    if (methodTree.getBody().getStatements().size() != 1) {
      return fromError(InlineValidationErrorReason.NOT_EXACTLY_ONE_STATEMENT);
    }

    MethodSymbol methodSym = getSymbol(methodTree);
    if (methodSym.getModifiers().contains(Modifier.PRIVATE)) {
      return fromError(InlineValidationErrorReason.API_IS_PRIVATE);
    }

    StatementTree statement = methodTree.getBody().getStatements().get(0);

    if (state.getSourceForNode(statement) == null) {
      return fromError(InlineValidationErrorReason.NO_BODY);
    }

    // we can only inline either a ExpressionStatementTree or a ReturnTree
    ExpressionTree body;
    // The statement is either an ExpressionStatement or a ReturnStatement, given
    // InlinabilityResult.forMethod
    switch (statement.getKind()) {
      case EXPRESSION_STATEMENT -> body = ((ExpressionStatementTree) statement).getExpression();
      case RETURN -> {
        body = ((ReturnTree) statement).getExpression();
        if (body == null) {
          return fromError(InlineValidationErrorReason.EMPTY_VOID);
        }
      }
      default -> {
        return fromError(InlineValidationErrorReason.COMPLEX_STATEMENT);
      }
    }

    if (body instanceof MethodInvocationTree methodInvocation
        && isSuper(methodInvocation.getMethodSelect())) {
      return fromError(InlineValidationErrorReason.NO_SUPER_CALLS);
    }

    if (methodSym.isVarArgs() && usesVarargsParamPoorly(body, methodSym.params().last(), state)) {
      return fromError(InlineValidationErrorReason.VARARGS_USED_UNSAFELY, body);
    }

    for (VarSymbol param : methodSym.params()) {
      if (matchesArgN(param.name.toString())) {
        return fromError(InlineValidationErrorReason.PARAM_NAMES_ARE_NAMED_ARGN);
      }
    }

    // TODO(kak): declare a list of all the types we don't want to allow (e.g., ClassTree) and use
    // contains
    if (body.toString().contains("{") || body.getKind() == Kind.CONDITIONAL_EXPRESSION) {
      return fromError(InlineValidationErrorReason.COMPLEX_STATEMENT, body);
    }

    Symbol usedMultipleTimes = usesVariablesMultipleTimes(body, methodSym.params(), state);
    if (usedMultipleTimes != null) {
      return fromError(
          InlineValidationErrorReason.REUSE_OF_ARGUMENTS, body, usedMultipleTimes.toString());
    }

    Tree privateOrDeprecatedApi =
        usesPrivateOrDeprecatedApis(body, state, getVisibility(methodSym));
    if (privateOrDeprecatedApi != null) {
      return fromError(
          InlineValidationErrorReason.CALLS_DEPRECATED_OR_PRIVATE_APIS,
          body,
          state.getSourceForNode(privateOrDeprecatedApi));
    }

    if (hasArgumentInPossiblyNonExecutedPosition(methodTree, body)) {
      return fromError(InlineValidationErrorReason.BODY_WOULD_EVALUATE_DIFFERENTLY, body);
    }

    if (methodCanBeOverridden(methodSym)) {
      // TODO(glorioso): One additional edge case we can check is if the owning class can't be
      // overridden due to having no publicly-accessible constructors.
      return fromError(
          methodSym.isDefault()
              ? InlineValidationErrorReason.METHOD_CAN_BE_OVERRIDDEN_AND_CANT_BE_FIXED
              : InlineValidationErrorReason.METHOD_CAN_BE_OVERRIDDEN_BUT_CAN_BE_FIXED,
          body);
    }

    return inlinable(body);
  }

  private static Symbol usesVariablesMultipleTimes(
      ExpressionTree body, List<VarSymbol> parameterVariables, VisitorState state) {
    AtomicReference<Symbol> usesVarsTwice = new AtomicReference<>();
    new TreePathScanner<Void, Void>() {
      final Set<Symbol> usedVariables = new HashSet<>();

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        Symbol usedSymbol = getSymbol(identifierTree);
        if (parameterVariables.contains(usedSymbol) && !usedVariables.add(usedSymbol)) {
          usesVarsTwice.set(usedSymbol);
        }
        return super.visitIdentifier(identifierTree, null);
      }
    }.scan(new TreePath(state.getPath(), body), null);
    return usesVarsTwice.get();
  }

  // If the body refers to the varargs value at all, it should only be as the last argument
  // in a method call that is *also* varargs.
  private static boolean usesVarargsParamPoorly(
      ExpressionTree expressionTree, VarSymbol varargsParam, VisitorState state) {
    AtomicBoolean usesVarargsPoorly = new AtomicBoolean(false);
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        if (!getSymbol(identifierTree).equals(varargsParam)) {
          return super.visitIdentifier(identifierTree, null);
        }
        Tree parentNode = getCurrentPath().getParentPath().getLeaf();
        if (!(parentNode instanceof MethodInvocationTree)) {
          usesVarargsPoorly.set(true);
          return null;
        }
        MethodInvocationTree mit = (MethodInvocationTree) parentNode;

        if (!getSymbol(mit).isVarArgs()) {
          // Passing varargs to another method that maybe takes an explicit array?
          usesVarargsPoorly.set(true);
          return null;
        }

        List<? extends ExpressionTree> args = mit.getArguments();
        if (args.isEmpty()) {
          // buh! confusing.
          return super.visitIdentifier(identifierTree, null);
        }

        int indexOfThisTreeUse = args.indexOf(identifierTree);
        if (indexOfThisTreeUse != args.size() - 1) {
          // Varargs not in position.
          usesVarargsPoorly.set(true);
          return null;
        }

        return super.visitIdentifier(identifierTree, null);
      }
    }.scan(new TreePath(state.getPath(), expressionTree), null);
    return usesVarargsPoorly.get();
  }

  private static Tree usesPrivateOrDeprecatedApis(
      ExpressionTree statement, VisitorState state, Visibility minVisibility) {
    AtomicReference<Tree> usesDeprecatedOrLessVisibleApis = new AtomicReference<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitLambdaExpression(LambdaExpressionTree lambda, Void unused) {
        // we override so we can ignore the lambda.getParameters()
        return super.scan(lambda.getBody(), null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelect, Void unused) {
        // This check is necessary as the TreeScanner doesn't visit the "name" part of the
        // left-hand of an assignment.
        if (isDeprecatedOrLessVisible(memberSelect, minVisibility)) {
          return null; // short-circuit
        }
        return super.visitMemberSelect(memberSelect, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        if (!isLocal(getSymbol(identifierTree))) {
          if (!identifierTree.getName().contentEquals("this")) {
            if (isDeprecatedOrLessVisible(identifierTree, minVisibility)) {
              return null; // short-circuit
            }
          }
        }
        return super.visitIdentifier(identifierTree, null);
      }

      @Override
      public Void visitNewClass(NewClassTree newClassTree, Void unused) {
        if (isDeprecatedOrLessVisible(newClassTree, minVisibility)) {
          return null; // short-circuit
        }
        return super.visitNewClass(newClassTree, null);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree methodInvocation, Void unused) {
        if (isDeprecatedOrLessVisible(methodInvocation, minVisibility)) {
          return null; // short-circuit
        }
        return super.visitMethodInvocation(methodInvocation, null);
      }

      private boolean isDeprecatedOrLessVisible(Tree tree, Visibility minVisibility) {
        Symbol sym = getSymbol(tree);
        Visibility visibility = getVisibility(sym);
        if (!(sym instanceof PackageSymbol) && visibility.compareTo(minVisibility) < 0) {
          usesDeprecatedOrLessVisibleApis.set(tree);
          return true;
        }
        if (hasAnnotation(sym, "java.lang.Deprecated", state)) {
          usesDeprecatedOrLessVisibleApis.set(tree);
          return true;
        }

        return false;
      }
    }.scan(statement, null);

    return usesDeprecatedOrLessVisibleApis.get();
  }

  private enum Visibility {
    PRIVATE,
    PACKAGE,
    PROTECTED,
    PUBLIC;
  }

  private static Visibility getVisibility(Symbol symbol) {
    if (symbol.getModifiers().contains(Modifier.PRIVATE)) {
      return Visibility.PRIVATE;
    } else if (symbol.getModifiers().contains(Modifier.PROTECTED)) {
      return Visibility.PROTECTED;
    } else if (symbol.getModifiers().contains(Modifier.PUBLIC)) {
      return Visibility.PUBLIC;
    } else {
      return Visibility.PACKAGE;
    }
  }

  private static boolean hasArgumentInPossiblyNonExecutedPosition(
      MethodTree methodTree, ExpressionTree statement) {
    AtomicBoolean paramReferred = new AtomicBoolean(false);
    ImmutableSet<VarSymbol> params =
        methodTree.getParameters().stream().map(ASTHelpers::getSymbol).collect(toImmutableSet());
    new TreeScanner<Void, Void>() {
      Tree currentContextTree = null;

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree lambda, Void unused) {
        Tree lastContext = currentContextTree;
        currentContextTree = lambda;
        scan(lambda.getBody(), null);
        currentContextTree = lastContext;
        return null;
      }

      @Override
      public Void visitConditionalExpression(ConditionalExpressionTree conditional, Void unused) {
        scan(conditional.getCondition(), null);
        // If the variables show up in the left or right side, they may conditionally not be
        // executed.
        Tree lastContext = currentContextTree;
        currentContextTree = conditional;
        scan(conditional.getTrueExpression(), null);
        scan(conditional.getFalseExpression(), null);
        currentContextTree = lastContext;
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifier, Void unused) {
        // If the lambda captures method parameters, inlining the method body can change the
        // timing of the evaluation of the arguments.
        if (currentContextTree != null && params.contains(getSymbol(identifier))) {
          paramReferred.set(true);
        }
        return super.visitIdentifier(identifier, null);
      }
    }.scan(statement, null);
    return paramReferred.get();
  }
}
