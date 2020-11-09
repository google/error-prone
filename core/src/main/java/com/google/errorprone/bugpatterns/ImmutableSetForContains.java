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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotationWithSimpleName;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isStatic;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * Refactoring to suggest using {@code private static final} {@link ImmutableSet} over {@link
 * ImmutableList} when using only contains, containsAll and isEmpty.
 */
@BugPattern(
    name = "ImmutableSetForContains",
    summary =
        "ImmutableSet is a more efficient type for private static final constants if the constant"
            + " is only used for contains, containsAll or isEmpty checks.",
    severity = SUGGESTION)
public final class ImmutableSetForContains extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<Tree> PRIVATE_STATIC_IMMUTABLE_LIST_MATCHER =
      allOf(
          isStatic(),
          hasModifier(Modifier.PRIVATE),
          hasModifier(Modifier.FINAL),
          isSameType(ImmutableList.class));

  private static final Matcher<Tree> EXCLUSIONS =
      anyOf(
          hasAnnotationWithSimpleName("Bind"),
          hasAnnotationWithSimpleName("Inject"));

  private static final Matcher<ExpressionTree> IMMUTABLE_LIST_FACTORIES =
      staticMethod().onClass(ImmutableList.class.getName()).namedAnyOf("of", "copyOf");
  private static final Matcher<ExpressionTree> IMMUTABLE_LIST_BUILD =
      instanceMethod().onExactClass(ImmutableList.Builder.class.getName()).namedAnyOf("build");
  private static final Matcher<ExpressionTree> IMMUTABLE_COLLECTION =
      instanceMethod().onExactClass(Stream.class.getName()).named("collect");

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableSet<VariableTree> immutableListVar =
        tree.getMembers().stream()
            .filter(member -> member.getKind().equals(Kind.VARIABLE))
            .map(VariableTree.class::cast)
            // TODO(user) : Expand to non-static vars with simple init in constructors.
            .filter(
                member ->
                    PRIVATE_STATIC_IMMUTABLE_LIST_MATCHER.matches(member, state)
                        && !EXCLUSIONS.matches(member, state))
            .collect(toImmutableSet());
    if (immutableListVar.isEmpty()) {
      return Description.NO_MATCH;
    }
    ImmutableVarUsageScanner usageScanner = new ImmutableVarUsageScanner(immutableListVar, state);
    usageScanner.scan(tree, state);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Optional<VariableTree> firstReplacement = Optional.empty();
    for (VariableTree var : immutableListVar) {
      if (!usageScanner.disallowedVarUsages.get(getSymbol(var))) {
        firstReplacement = Optional.of(var);
        fix.merge(convertListToSetInit(var, state));
      }
    }
    if (!firstReplacement.isPresent()) {
      return Description.NO_MATCH;
    }
    return describeMatch(firstReplacement.get(), fix.build());
  }

  private static SuggestedFix convertListToSetInit(VariableTree var, VisitorState state) {
    Tree immutableListTypeTree =
        var.getType().getKind().equals(Kind.PARAMETERIZED_TYPE)
            ? ((ParameterizedTypeTree) var.getType()).getType()
            : var.getType();
    SuggestedFix.Builder fix =
        SuggestedFix.builder()
            .addImport(ImmutableSet.class.getName())
            .replace(immutableListTypeTree, "ImmutableSet");
    if (IMMUTABLE_LIST_FACTORIES.matches(var.getInitializer(), state)) {
      fix.replace(getReceiver(var.getInitializer()), "ImmutableSet");
      return fix.build();
    }
    if (IMMUTABLE_COLLECTION.matches(var.getInitializer(), state)) {
      fix.addStaticImport("com.google.common.collect.ImmutableSet.toImmutableSet")
          .replace(
              getOnlyElement(((MethodInvocationTree) var.getInitializer()).getArguments()),
              "toImmutableSet()");
      return fix.build();
    }
    if (IMMUTABLE_LIST_BUILD.matches(var.getInitializer(), state)) {
      Optional<ExpressionTree> typeTree =
          getRootMethod((MethodInvocationTree) var.getInitializer(), state);
      if (typeTree.isPresent()) {
        fix.replace(typeTree.get(), "ImmutableSet");
        return fix.build();
      }
    }
    return fix.replace(
            var.getInitializer(),
            "ImmutableSet.copyOf(" + state.getSourceForNode(var.getInitializer()) + ")")
        .build();
  }

  private static Optional<ExpressionTree> getRootMethod(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree receiver = getReceiver(methodInvocationTree);
    while (receiver != null && isSameType(ImmutableList.Builder.class).matches(receiver, state)) {
      receiver = getReceiver(receiver);
    }
    return Optional.ofNullable(receiver)
        .filter(tree -> isSameType(ImmutableList.class).matches(tree, state));
  }

  // Scans the tree for all usages of immutable list vars and determines if any usage will prevent
  // us from turning ImmutableList into ImmutableSet.
  private static final class ImmutableVarUsageScanner extends TreeScanner<Void, VisitorState> {

    private static final Matcher<ExpressionTree> ALLOWED_FUNCTIONS_ON_LIST =
        instanceMethod()
            .onExactClass(ImmutableList.class.getName())
            .namedAnyOf("contains", "containsAll", "isEmpty");
    private static final Matcher<ExpressionTree> ALLOWED_FUNCTIONS_WHEN_UNIQUE =
        instanceMethod()
            .onExactClass(ImmutableList.class.getName())
            .namedAnyOf("size", "stream", "iterator", "forEach");
    private static final Matcher<ExpressionTree> IMMUTABLE_LIST_OF =
        staticMethod().onClass(ImmutableList.class.getName()).named("of");

    // For each ImmutableList usage var, we will keep a map indicating if the var has been used in a
    // way that would prevent us from making it a set.
    private final Map<Symbol, Boolean> disallowedVarUsages;
    private final ImmutableMap<Symbol, Boolean> hasUniqueElements;

    private ImmutableVarUsageScanner(
        ImmutableSet<VariableTree> immutableListVar, VisitorState state) {
      this.disallowedVarUsages =
          immutableListVar.stream()
              .map(ASTHelpers::getSymbol)
              .collect(toMap(x -> x, x -> Boolean.FALSE));
      this.hasUniqueElements =
          immutableListVar.stream()
              .collect(toImmutableMap(ASTHelpers::getSymbol, var -> hasUniqueElements(var, state)));
    }

    @Override
    public Void visitMethodInvocation(
        MethodInvocationTree methodInvocationTree, VisitorState visitorState) {
      ExpressionTree receiver = getReceiver(methodInvocationTree);
      if (receiver == null || !receiver.getKind().equals(Kind.IDENTIFIER)) {
        return super.visitMethodInvocation(methodInvocationTree, visitorState);
      }
      if (!disallowedVarUsages.containsKey(getSymbol(receiver))) {
        // Not a function invocation on any of the candidate immutable list vars.
        return super.visitMethodInvocation(methodInvocationTree, visitorState);
      }
      if (!ALLOWED_FUNCTIONS_ON_LIST.matches(methodInvocationTree, visitorState)
          && !(ALLOWED_FUNCTIONS_WHEN_UNIQUE.matches(methodInvocationTree, visitorState)
              && hasUniqueElements.get(getSymbol(receiver)))) {
        return super.visitMethodInvocation(methodInvocationTree, visitorState);
      }
      return null;
    }

    @Override
    public Void visitEnhancedForLoop(
        EnhancedForLoopTree enhancedForLoopTree, VisitorState visitorState) {
      ExpressionTree expression = enhancedForLoopTree.getExpression();
      if (!expression.getKind().equals(Kind.IDENTIFIER)) {
        return super.visitEnhancedForLoop(enhancedForLoopTree, visitorState);
      }
      Symbol var = getSymbol(expression);
      if (!disallowedVarUsages.containsKey(var) || !hasUniqueElements.containsKey(var)) {
        return super.visitEnhancedForLoop(enhancedForLoopTree, visitorState);
      }
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree identifierTree, VisitorState visitorState) {
      disallowedVarUsages.computeIfPresent(
          getSymbol(identifierTree), (sym, oldVal) -> Boolean.TRUE);
      return super.visitIdentifier(identifierTree, visitorState);
    }

    private static boolean hasUniqueElements(VariableTree varTree, VisitorState state) {
      if (varTree.getInitializer() == null) {
        return false;
      }
      if (!IMMUTABLE_LIST_OF.matches(varTree.getInitializer(), state)) {
        return false;
      }
      List<? extends ExpressionTree> initElements =
          ((MethodInvocationTree) varTree.getInitializer()).getArguments();
      return areDistinctConstantVals(initElements) || areDistinctEnums(initElements);
    }

    private static boolean areDistinctConstantVals(List<? extends ExpressionTree> trees) {
      if (trees.stream().anyMatch(tree -> constValue(tree) == null)) {
        return false;
      }
      return trees.stream().map(ASTHelpers::constValue).distinct().count() == trees.size();
    }

    private static boolean areDistinctEnums(List<? extends ExpressionTree> trees) {
      if (trees.stream().anyMatch(tree -> getSymbol(tree) == null)) {
        return false;
      }
      ImmutableList<Symbol> symbols =
          trees.stream().map(ASTHelpers::getSymbol).collect(toImmutableList());
      if (!symbols.stream().allMatch(sym -> sym.getKind().equals(ElementKind.ENUM_CONSTANT))) {
        return false;
      }
      return ImmutableSet.copyOf(symbols).size() == trees.size();
    }
  }
}
