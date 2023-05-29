/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/**
 * Identifies {@code RuleChain} class fields and proposes refactoring to ordered {@code @Rule(order
 * = n)}.
 */
@BugPattern(
    tags = {StandardTags.REFACTORING},
    summary =
        "Prefer using `@Rule` with an explicit order over declaring a `RuleChain`. "
            + "RuleChain was the only way to declare ordered rules before JUnit 4.13. Newer "
            + "versions should use the cleaner individual `@Rule(order = n)` option.",
    severity = WARNING)
public class DoNotUseRuleChain extends BugChecker implements VariableTreeMatcher {

  private static final String TEST_RULE_VAR_PREFIX = "testRule";

  private static final String JUNIT_RULE_CHAIN_IMPORT_PATH = "org.junit.rules.RuleChain";
  private static final String JUNIT_CLASS_RULE_IMPORT_PATH = "org.junit.ClassRule";
  private static final String JUNIT_RULE_IMPORT_PATH = "org.junit.Rule";

  private static final Matcher<ExpressionTree> RULE_CHAIN_METHOD_MATCHER =
      anyMethod().onDescendantOf(JUNIT_RULE_CHAIN_IMPORT_PATH).namedAnyOf("around", "outerRule");

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol.getKind() != ElementKind.FIELD
        || !isRuleChainExpression(tree, state)
        || !isClassWithSingleRule(state)
        || isChainedRuleChain(tree, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, refactor(tree, state));
  }

  private static boolean isRuleChainExpression(VariableTree tree, VisitorState state) {
    ExpressionTree expression = tree.getInitializer();
    if (expression == null) {
      return false;
    }
    return RULE_CHAIN_METHOD_MATCHER.matches(expression, state);
  }

  /**
   * This refactoring only matches classes that feature a single occurrence of a {@code @Rule}. This
   * is done to avoid breaking some edge cases where more than one RuleChain might be declared or a
   * mixture of {@code @Rule} and {@code @RuleChain}.
   */
  private static boolean isClassWithSingleRule(VisitorState state) {
    Optional<ClassTree> classTree = getClassTree(state);
    if (classTree.isEmpty()) {
      return false;
    }
    return classTree.get().getMembers().stream()
            .filter(tree -> ASTHelpers.hasAnnotation(tree, JUNIT_RULE_IMPORT_PATH, state))
            .count()
        == 1;
  }

  private static Optional<ClassTree> getClassTree(VisitorState state) {
    return stream(state.getPath().iterator())
        .filter(parent -> parent.getKind() == Kind.CLASS)
        .map(ClassTree.class::cast)
        .findFirst();
  }

  /**
   * Don't evaluate if there is a {@code RuleChain} expression inside a {@code
   * RuleChain.outerRule()} or {@code RuleChain.around()} method.
   */
  private static boolean isChainedRuleChain(VariableTree tree, VisitorState state) {
    return getOrderedExpressions(tree, state).stream()
        .map(DoNotUseRuleChain::getArgumentExpression)
        .anyMatch(ex -> Matchers.isSameType(JUNIT_RULE_CHAIN_IMPORT_PATH).matches(ex, state));
  }

  private static SuggestedFix refactor(VariableTree tree, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    ImmutableList<ExpressionTree> expressions = getOrderedExpressions(tree, state);

    String replacement = "";
    for (int i = 0; i < expressions.size(); i++) {
      ExpressionTree expression = getArgumentExpression(expressions.get(i));
      addImportIfNecessary(expression, fix);
      replacement += extractRuleFromExpression(expression, i, tree, state);
    }

    fix.removeImport(JUNIT_RULE_CHAIN_IMPORT_PATH);
    fix.replace(tree, replacement);
    return fix.build();
  }

  /**
   * Since {@code ASTHelpers.getReceiver()} goes into reverse order, the expression list must be
   * reversed in order for them to follow the required ordering from {@code @Rule(order = n)}.
   */
  private static ImmutableList<ExpressionTree> getOrderedExpressions(
      VariableTree tree, VisitorState state) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    for (ExpressionTree ex = tree.getInitializer();
        RULE_CHAIN_METHOD_MATCHER.matches(ex, state);
        ex = ASTHelpers.getReceiver(ex)) {
      expressions.add(ex);
    }
    return expressions.build().reverse();
  }

  // Gets the only argument from {@code RuleChain.outerRule()} or {@code RuleChain.around()} to use
  // as the variable value from the new ordered {@code @Rule(order = n)}
  private static ExpressionTree getArgumentExpression(ExpressionTree ex) {
    MethodInvocationTree methodInvocation = (MethodInvocationTree) ex;
    return methodInvocation.getArguments().get(0);
  }

  private static void addImportIfNecessary(ExpressionTree expression, SuggestedFix.Builder fix) {
    Type originalType = ASTHelpers.getResultType(expression);
    if (ImmutableSet.of(Kind.METHOD_INVOCATION, Kind.LAMBDA_EXPRESSION)
        .contains(expression.getKind())) {
      fix.addImport(originalType.tsym.getQualifiedName().toString());
    }
  }

  private static String extractRuleFromExpression(
      ExpressionTree expression, int order, VariableTree tree, VisitorState state) {
    String className = className(expression);

    return String.format(
        "%s(order = %d)\npublic %sfinal %s %s = %s;\n",
        annotationName(tree, state),
        order,
        ASTHelpers.getSymbol(tree).isStatic() ? "static " : "",
        className,
        classToVariableName(className),
        state.getSourceForNode(expression));
  }

  private static String className(ExpressionTree expression) {
    Type originalType = ASTHelpers.getResultType(expression);
    List<Type> arguments = originalType.getTypeArguments();
    String className = originalType.tsym.getSimpleName().toString();
    if (!arguments.isEmpty()) {
      String argumentString =
          arguments.stream().map(t -> t.tsym.getSimpleName().toString()).collect(joining(", "));
      return String.format("%s<%s>", className, argumentString);
    }
    return className;
  }

  private static String annotationName(VariableTree tree, VisitorState state) {
    if (ASTHelpers.hasAnnotation(tree, JUNIT_CLASS_RULE_IMPORT_PATH, state)) {
      return "@ClassRule";
    }
    return "@Rule";
  }

  private static String classToVariableName(String className) {
    return String.format("%s%s", TEST_RULE_VAR_PREFIX, className)
        .replace("<", "")
        .replace(">", "")
        .replace(",", "")
        .replace(" ", "");
  }
}
