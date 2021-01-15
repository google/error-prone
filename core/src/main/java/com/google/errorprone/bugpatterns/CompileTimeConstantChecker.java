/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher.hasCompileTimeConstantAnnotation;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.ElementKind;

/**
 * Detects invocations of methods with a parameter annotated {@code @CompileTimeConstant} such that
 * the corresponding actual parameter is not a compile-time constant expression, and initialisation
 * of fields declared {@code @CompileTimeConstant final} such that the actual value is not a
 * compile-time constant expression.
 *
 * <p>This type annotation checker enforces that for all method and constructor invocations, for all
 * formal parameters of the invoked method/constructor that are annotated with the {@link
 * com.google.errorprone.annotations.CompileTimeConstant} type annotation, the corresponding actual
 * parameter is an expression that satisfies one of the following conditions:
 *
 * <ol>
 *   <li>The expression is one for which the Java compiler can determine a constant value at compile
 *       time, or
 *   <li>the expression consists of the literal {@code null}, or
 *   <li>the expression consists of a single identifier, where the identifier is a formal method
 *       parameter or class field that is declared {@code final} and has the {@link
 *       com.google.errorprone.annotations.CompileTimeConstant} annotation.
 * </ol>
 *
 * <p>This type annotation checker also enforces that for all field declarations annotated with the
 * {@link com.google.errorprone.annotations.CompileTimeConstant} type annotation, the field is also
 * declared {@code final} and the corresponding initialised value satifsies one of the following
 * conditions:
 *
 * <ol>
 *   <li>The expression is one for which the Java compiler can determine a constant value at compile
 *       time, or
 *   <li>the expression consists of the literal {@code null}, or
 *   <li>the expression consists of a single identifier, where the identifier is a formal method
 *       parameter or class field that is declared {@code final} and has the {@link
 *       com.google.errorprone.annotations.CompileTimeConstant} annotation.
 * </ol>
 *
 * @see CompileTimeConstantExpressionMatcher
 */
@BugPattern(
    name = "CompileTimeConstant",
    summary =
        "Non-compile-time constant expression passed to parameter with "
            + "@CompileTimeConstant type annotation.",
    linkType = NONE,
    severity = ERROR)
public class CompileTimeConstantChecker extends BugChecker
    implements LambdaExpressionTreeMatcher,
        MemberReferenceTreeMatcher,
        MethodInvocationTreeMatcher,
        MethodTreeMatcher,
        NewClassTreeMatcher,
        VariableTreeMatcher,
        AssignmentTreeMatcher {

  private static final String DID_YOU_MEAN_FINAL_FMT_MESSAGE = " Did you mean to make '%s' final?";

  private final Matcher<ExpressionTree> compileTimeConstExpressionMatcher =
      new CompileTimeConstantExpressionMatcher();

  private final boolean checkFieldInitializers;

  public CompileTimeConstantChecker(ErrorProneFlags flags) {
    this.checkFieldInitializers =
        flags.getBoolean("CompileTimeConstantChecker:CheckFieldInitializers").orElse(true);
  }

  /**
   * Matches formal parameters with {@link com.google.errorprone.annotations.CompileTimeConstant}
   * annotations against corresponding actual parameters.
   *
   * @param state the visitor state
   * @param calleeSymbol the method whose formal parameters to consider
   * @param actualParams the list of actual parameters
   * @return a {@code Description} of the match <i>iff</i> for any of the actual parameters that is
   *     annotated with {@link com.google.errorprone.annotations.CompileTimeConstant}, the
   *     corresponding formal parameter is not a compile-time-constant expression in the sense of
   *     {@link CompileTimeConstantExpressionMatcher}. Otherwise returns {@code
   *     Description.NO_MATCH}.
   */
  private Description matchArguments(
      VisitorState state,
      final Symbol.MethodSymbol calleeSymbol,
      Iterator<? extends ExpressionTree> actualParams) {
    Symbol.VarSymbol lastFormalParam = null;
    for (Symbol.VarSymbol formalParam : calleeSymbol.getParameters()) {
      lastFormalParam = formalParam;
      // It appears that for some reason, the Tree for implicit Enum constructors
      // includes an invocation of super(), but the target symbol has the signature
      // Enum(String, int). This resulted in NoSuchElementExceptions.
      // It is safe to return no match in this case, since even if this could happen
      // in another scenario, a non-existent actual parameter can't possibly
      // be a non-constant parameter for a @CompileTimeConstant formal.
      if (!actualParams.hasNext()) {
        return Description.NO_MATCH;
      }
      ExpressionTree actualParam = actualParams.next();
      if (hasCompileTimeConstantAnnotation(state, formalParam)) {
        if (!compileTimeConstExpressionMatcher.matches(actualParam, state)) {
          return handleMatch(actualParam, state);
        }
      }
    }

    // If the last formal parameter is a vararg and has the @CompileTimeConstant annotation,
    // we need to check the remaining args as well.
    if (lastFormalParam == null) {
      return Description.NO_MATCH;
    }
    if (!calleeSymbol.isVarArgs()) {
      return Description.NO_MATCH;
    }
    if (!hasCompileTimeConstantAnnotation(state, lastFormalParam)) {
      return Description.NO_MATCH;
    }
    while (actualParams.hasNext()) {
      ExpressionTree actualParam = actualParams.next();
      if (!compileTimeConstExpressionMatcher.matches(actualParam, state)) {
        return handleMatch(actualParam, state);
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * If the non-constant variable is annotated with @CompileTimeConstant, it must have been
   * non-final. Suggest making it final in the error message.
   */
  private Description handleMatch(ExpressionTree actualParam, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(actualParam);
    if (!(sym instanceof VarSymbol)) {
      return describeMatch(actualParam);
    }
    VarSymbol var = (VarSymbol) sym;
    if (!hasCompileTimeConstantAnnotation(state, var)) {
      return describeMatch(actualParam);
    }
    return buildDescription(actualParam)
        .setMessage(
            this.message() + String.format(DID_YOU_MEAN_FINAL_FMT_MESSAGE, var.getSimpleName()))
        .build();
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Symbol.MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    return matchArguments(state, sym, tree.getArguments().iterator());
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Symbol.MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    return matchArguments(state, sym, tree.getArguments().iterator());
  }

  @Override
  public Description matchMethod(MethodTree node, VisitorState state) {
    Symbol.MethodSymbol method = ASTHelpers.getSymbol(node);
    if (method == null) {
      return Description.NO_MATCH;
    }
    List<Integer> compileTimeConstantAnnotationIndexes =
        getAnnotatedParams(method.getParameters(), state);
    if (compileTimeConstantAnnotationIndexes.isEmpty()) {
      return Description.NO_MATCH;
    }
    return checkSuperMethods(
        node,
        state,
        compileTimeConstantAnnotationIndexes,
        ASTHelpers.findSuperMethods(method, state.getTypes()));
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree node, VisitorState state) {
    Symbol.MethodSymbol sym = ASTHelpers.getSymbol(node);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    List<Integer> compileTimeConstantAnnotationIndexes =
        getAnnotatedParams(sym.getParameters(), state);
    if (compileTimeConstantAnnotationIndexes.isEmpty()) {
      return Description.NO_MATCH;
    }
    return checkLambda(node, state, compileTimeConstantAnnotationIndexes);
  }

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree node, VisitorState state) {
    List<Integer> compileTimeConstantAnnotationIndexes =
        getAnnotatedParams(
            node.getParameters().stream().map(ASTHelpers::getSymbol).collect(toImmutableList()),
            state);
    if (compileTimeConstantAnnotationIndexes.isEmpty()) {
      return Description.NO_MATCH;
    }
    return checkLambda(node, state, compileTimeConstantAnnotationIndexes);
  }

  @Override
  public Description matchVariable(VariableTree node, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (!hasCompileTimeConstantAnnotation(state, symbol)) {
      return Description.NO_MATCH;
    }
    switch (symbol.getKind()) {
      case PARAMETER:
        return Description.NO_MATCH;
      case FIELD:
        break; // continue below
      case LOCAL_VARIABLE: // disallowed by @Target meta-annotation
      default: // impossible
        throw new AssertionError(symbol.getKind());
    }
    if ((symbol.flags() & Flags.FINAL) == 0) {
      return buildDescription(node)
          .setMessage(
              this.message()
                  + String.format(DID_YOU_MEAN_FINAL_FMT_MESSAGE, symbol.getSimpleName()))
          .build();
    }
    if (checkFieldInitializers
        && node.getInitializer() != null
        && !compileTimeConstExpressionMatcher.matches(node.getInitializer(), state)) {
      return describeMatch(node.getInitializer());
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchAssignment(AssignmentTree node, VisitorState state) {
    ExpressionTree variable = node.getVariable();
    ExpressionTree expression = node.getExpression();
    Symbol assignedSymbol = ASTHelpers.getSymbol(variable);
    if (assignedSymbol == null || assignedSymbol.owner == null) {
      return Description.NO_MATCH;
    }
    if (assignedSymbol.owner.getKind() != ElementKind.CLASS) {
      return Description.NO_MATCH;
    }
    if (!hasCompileTimeConstantAnnotation(state, assignedSymbol)) {
      return Description.NO_MATCH;
    }
    if (compileTimeConstExpressionMatcher.matches(expression, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(expression);
  }

  private Description checkLambda(
      ExpressionTree node, VisitorState state, List<Integer> compileTimeConstantAnnotationIndexes) {
    MethodSymbol descriptorSymbol =
        (MethodSymbol) state.getTypes().findDescriptorSymbol(ASTHelpers.getType(node).tsym);
    ImmutableSet.Builder<Symbol.MethodSymbol> methods = ImmutableSet.builder();
    methods.add(descriptorSymbol);
    methods.addAll(ASTHelpers.findSuperMethods(descriptorSymbol, state.getTypes()));
    return checkSuperMethods(node, state, compileTimeConstantAnnotationIndexes, methods.build());
  }

  private List<Integer> getAnnotatedParams(List<VarSymbol> params, VisitorState state) {
    List<Integer> compileTimeConstantAnnotationIndexes = new ArrayList<>();
    for (int i = 0; i < params.size(); i++) {
      if (hasCompileTimeConstantAnnotation(state, params.get(i))) {
        compileTimeConstantAnnotationIndexes.add(i);
      }
    }
    return compileTimeConstantAnnotationIndexes;
  }

  private Description checkSuperMethods(
      Tree node,
      VisitorState state,
      List<Integer> compileTimeConstantAnnotationIndexes,
      Iterable<MethodSymbol> superMethods) {
    for (Symbol.MethodSymbol superMethod : superMethods) {
      for (Integer index : compileTimeConstantAnnotationIndexes) {
        if (!hasCompileTimeConstantAnnotation(state, superMethod.getParameters().get(index))) {
          return buildDescription(node)
              .setMessage(
                  "Method with @CompileTimeConstant parameter can't override method without it.")
              .build();
        }
      }
    }
    return Description.NO_MATCH;
  }
}
