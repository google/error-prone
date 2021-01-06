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
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

/**
 * Refactoring to suggest using {@code private static final} {@link ImmutableSet} over {@link
 * ImmutableList} when using only contains, containsAll and isEmpty.
 */
@BugPattern(
    name = "ImmutableSetForContains",
    summary =
        "This private static ImmutableList is only used for contains, containsAll or isEmpty"
            + " checks; prefer ImmutableSet.",
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
  private static final Matcher<ExpressionTree> IMMUTABLE_BUILDER_METHODS =
      instanceMethod()
          .onExactClass(ImmutableList.Builder.class.getName())
          .namedAnyOf("add", "addAll");

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableSet<VariableTree> immutableListVar =
        tree.getMembers().stream()
            .filter(member -> member.getKind().equals(Kind.VARIABLE))
            .map(VariableTree.class::cast)
            // TODO(ashishkedia) : Expand to non-static vars with simple init in constructors.
            .filter(
                member ->
                    PRIVATE_STATIC_IMMUTABLE_LIST_MATCHER.matches(member, state)
                        && !EXCLUSIONS.matches(member, state))
            .collect(toImmutableSet());
    if (immutableListVar.isEmpty()) {
      return Description.NO_MATCH;
    }
    ImmutableVarUsageScanner usageScanner = new ImmutableVarUsageScanner(immutableListVar);
    // Scan entire compilation unit since private static vars of nested classes may be referred in
    // parent class.
    TreePath cuPath = state.findPathToEnclosing(CompilationUnitTree.class);
    usageScanner.scan(cuPath.getLeaf(), state.withPath(cuPath));
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Optional<VariableTree> firstReplacement = Optional.empty();
    for (VariableTree var : immutableListVar) {
      if (isSuppressed(var)) {
        continue;
      }
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
    SuggestedFix.Builder fix =
        SuggestedFix.builder()
            .addImport(ImmutableSet.class.getName())
            .replace(stripParameters(var.getType()), "ImmutableSet");
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
      Optional<ExpressionTree> rootExpr =
          getRootMethod((MethodInvocationTree) var.getInitializer(), state);
      if (rootExpr.isPresent()) {
        if (rootExpr.get().getKind().equals(Kind.METHOD_INVOCATION)) {
          MethodInvocationTree methodTree = (MethodInvocationTree) rootExpr.get();
          fix.replace(getReceiver(methodTree), "ImmutableSet");
          return fix.build();
        }
        if (rootExpr.get().getKind().equals(Kind.NEW_CLASS)) {
          NewClassTree ctorTree = (NewClassTree) rootExpr.get();
          fix.replace(stripParameters(ctorTree.getIdentifier()), "ImmutableSet.Builder");
        }
        return fix.build();
      }
    }
    return fix.replace(
            var.getInitializer(),
            "ImmutableSet.copyOf(" + state.getSourceForNode(var.getInitializer()) + ")")
        .build();
  }

  private static Tree stripParameters(Tree tree) {
    return tree.getKind().equals(Kind.PARAMETERIZED_TYPE)
        ? ((ParameterizedTypeTree) tree).getType()
        : tree;
  }

  private static Optional<ExpressionTree> getRootMethod(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree receiver = getReceiver(methodInvocationTree);
    while (receiver != null && IMMUTABLE_BUILDER_METHODS.matches(receiver, state)) {
      receiver = getReceiver(receiver);
    }
    return Optional.ofNullable(receiver);
  }

  // Scans the tree for all usages of immutable list vars and determines if any usage will prevent
  // us from turning ImmutableList into ImmutableSet.
  private static final class ImmutableVarUsageScanner extends TreeScanner<Void, VisitorState> {

    private static final Matcher<ExpressionTree> ALLOWED_FUNCTIONS_ON_LIST =
        instanceMethod()
            .onExactClass(ImmutableList.class.getName())
            .namedAnyOf("contains", "containsAll", "isEmpty");

    // For each ImmutableList usage var, we will keep a map indicating if the var has been used in a
    // way that would prevent us from making it a set.
    private final Map<Symbol, Boolean> disallowedVarUsages;

    private ImmutableVarUsageScanner(ImmutableSet<VariableTree> immutableListVar) {
      this.disallowedVarUsages =
          immutableListVar.stream()
              .map(ASTHelpers::getSymbol)
              .collect(toMap(x -> x, x -> Boolean.FALSE));
    }

    @Override
    public Void visitMethodInvocation(
        MethodInvocationTree methodInvocationTree, VisitorState state) {
      methodInvocationTree.getArguments().forEach(tree -> scan(tree, state));
      methodInvocationTree.getTypeArguments().forEach(tree -> scan(tree, state));
      if (!allowedFuncOnImmutableVar(methodInvocationTree, state)) {
        scan(methodInvocationTree.getMethodSelect(), state);
      }
      return null;
    }

    private boolean allowedFuncOnImmutableVar(MethodInvocationTree methodTree, VisitorState state) {
      ExpressionTree receiver = getReceiver(methodTree);
      if (receiver == null) {
        return false;
      }
      if (!disallowedVarUsages.containsKey(getSymbol(receiver))) {
        // Not a function invocation on any of the candidate immutable list vars.
        return false;
      }
      return ALLOWED_FUNCTIONS_ON_LIST.matches(methodTree, state);
    }

    @Override
    public Void visitIdentifier(IdentifierTree identifierTree, VisitorState visitorState) {
      recordDisallowedUsage(getSymbol(identifierTree));
      return super.visitIdentifier(identifierTree, visitorState);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree memberSelectTree, VisitorState visitorState) {
      recordDisallowedUsage(getSymbol(memberSelectTree));
      return super.visitMemberSelect(memberSelectTree, visitorState);
    }

    private void recordDisallowedUsage(Symbol symbol) {
      disallowedVarUsages.computeIfPresent(symbol, (sym, oldVal) -> Boolean.TRUE);
    }
  }
}
