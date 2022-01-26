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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.fixes.SuggestedFixes.AdditionPosition;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.util.Name;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Anytime you need to look up a constant value from VisitorState, improve performance by"
            + " creating a cache for it with VisitorState.memoize",
    severity = SeverityLevel.WARNING)
public class MemoizeConstantVisitorStateLookups extends BugChecker
    implements CompilationUnitTreeMatcher {

  private static final Matcher<ExpressionTree> CONSTANT_LOOKUP =
      instanceMethod()
          .onExactClass(VisitorState.class.getName())
          .namedAnyOf("getName", "getTypeFromString", "getSymbolFromString")
          .withParameters(String.class.getName());

  private static final Matcher<ExpressionTree> MEMOIZE_CALL =
      staticMethod().onClass(VisitorState.class.getName()).named("memoize");

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    tree.getTypeDecls().stream()
        .filter(t -> t instanceof ClassTree)
        .forEach(t -> state.reportMatch(fixLookupsInClass((ClassTree) t, state)));
    return NO_MATCH;
  }

  private Description fixLookupsInClass(ClassTree tree, VisitorState state) {
    ImmutableList<CallSite> lookups = findConstantLookups(tree, state);
    if (lookups.isEmpty()) {
      return NO_MATCH;
    }
    Map<String, Map<Name, List<CallSite>>> groupedCallSites =
        lookups.stream()
            .collect(
                groupingBy(
                    callSite -> callSite.argumentValue, groupingBy(callSite -> callSite.method)));

    // addMembers can only be called once per class, so we emit just one fix for all occurrences.
    SuggestedFix.Builder fix = SuggestedFix.builder();
    ImmutableSortedSet.Builder<String> membersToAdd =
        new ImmutableSortedSet.Builder<>(naturalOrder());
    for (Map.Entry<String, Map<Name, List<CallSite>>> lookup : groupedCallSites.entrySet()) {
      String argument = lookup.getKey();
      Map<Name, List<CallSite>> usages = lookup.getValue();
      if (usages.size() == 1) {
        // The common case: we have state.foo(argument), and never state.bar(argument), so we can
        // name the constant after just argument.
        Map.Entry<Name, List<CallSite>> useSites = usages.entrySet().iterator().next();
        Name methodName = useSites.getKey();
        List<CallSite> instances = useSites.getValue();
        memoizeSupplier(
            state, fix, membersToAdd::add, argument, methodName, instances, (name, type) -> name);
      } else {
        // Sadly we have both state.foo(argument) and also state.bar(argument), so we need two
        // constants based on argument and must disambiguate their names.
        for (Map.Entry<Name, List<CallSite>> usage : usages.entrySet()) {
          Name methodName = usage.getKey();
          List<CallSite> instances = usage.getValue();
          memoizeSupplier(
              state,
              fix,
              membersToAdd::add,
              argument,
              methodName,
              instances,
              (name, type) -> name + "_" + type);
        }
      }
    }

    SuggestedFixes.addMembers(tree, state, AdditionPosition.LAST, membersToAdd.build())
        .ifPresent(fix::merge);

    // Report the fix on the first of the call site trees.
    MethodInvocationTree fixTree =
        lookups.stream()
            .map(cs -> cs.entireTree)
            .min(comparingInt(ASTHelpers::getStartPosition))
            .get(); // Always succeeds, lookups is not empty.
    return describeMatch(fixTree, fix.build());
  }

  private static final class CallSite {
    /** The method on VisitorState being called. */
    final Name method;
    /** The compile-time constant value being passed to that method. */
    final String argumentValue;
    /** The actual expression with that value: a string literal, or a constant with such a value. */
    final ExpressionTree argumentExpression;
    /** The entire invocation of the VisitorState method. */
    final MethodInvocationTree entireTree;

    CallSite(
        Name method,
        String argumentValue,
        ExpressionTree argumentExpression,
        MethodInvocationTree entireTree) {
      this.method = method;
      this.argumentValue = argumentValue;
      this.argumentExpression = argumentExpression;
      this.entireTree = entireTree;
    }
  }

  private static ImmutableList<CallSite> findConstantLookups(ClassTree tree, VisitorState state) {
    ImmutableList.Builder<CallSite> result = ImmutableList.builder();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (CONSTANT_LOOKUP.matches(tree, state)) {
          handleConstantLookup(tree);
        } else if (MEMOIZE_CALL.matches(tree, state)) {
          // Don't descend into calls to memoize, because they're already properly memoized!
          return null;
        }
        return super.visitMethodInvocation(tree, null);
      }

      /** Adds to result if the call uses a compile-time constant argument. */
      private void handleConstantLookup(MethodInvocationTree tree) {
        ExpressionTree argumentExpr = tree.getArguments().get(0);
        String argumentValue = ASTHelpers.constValue(argumentExpr, String.class);
        if (argumentValue != null) {
          ExpressionTree methodSelect = tree.getMethodSelect();
          if (methodSelect instanceof JCFieldAccess) {
            JCFieldAccess fieldAccess = (JCFieldAccess) methodSelect;
            Name method = fieldAccess.name;
            result.add(new CallSite(method, argumentValue, argumentExpr, tree));
          } else {
            // Just give up on calls we can't understand - maybe from inside VisitorState itself?
          }
        }
      }
    }.scan(tree, null);
    return result.build();
  }

  /**
   * Adds to {@code fix} the changes necessary to memoize all the callsites in {@code instances},
   * and offers {@code memberConsumer} a new constant to which the fixed callsites will refer.
   */
  private static void memoizeSupplier(
      VisitorState state,
      SuggestedFix.Builder fix,
      Consumer<String> memberConsumer,
      String argument,
      Name methodName,
      List<CallSite> instances,
      BiFunction<String, String, String> namingStrategy) {

    CallSite prototype = bestCallsite(instances);
    MethodSymbol sym = ASTHelpers.getSymbol(prototype.entireTree);
    TypeSymbol returnType = sym.getReturnType().tsym;
    String returnTypeName = returnType.getSimpleName().toString();
    String newConstantPrefix = Ascii.toUpperCase(argument).replaceAll("\\W", "_");
    String newConstantName =
        namingStrategy.apply(newConstantPrefix, Ascii.toUpperCase(returnTypeName));

    memberConsumer.accept(
        String.format(
            "private static final %s<%s> %s = %s.memoize(state -> state.%s(%s));",
            SuggestedFixes.qualifyType(state, fix, Supplier.class.getCanonicalName()),
            SuggestedFixes.qualifyType(state, fix, returnType.getQualifiedName().toString()),
            newConstantName,
            SuggestedFixes.qualifyType(state, fix, VisitorState.class.getCanonicalName()),
            methodName,
            state.getSourceForNode(prototype.argumentExpression)));
    for (CallSite instance : instances) {
      ExpressionTree visitorStateExpr = ASTHelpers.getReceiver(instance.entireTree);
      fix.replace(
          instance.entireTree,
          String.format("%s.get(%s)", newConstantName, state.getSourceForNode(visitorStateExpr)));
    }
  }

  /**
   * Chooses one call site to use for understanding all related callsites. This is used to determine
   * what type is being looked up and what source text to use as the argument to the VisitorState
   * method. The idea is that if someone writes both getName("foo") and getName(FOO), we want to
   * only define one constant for them. In case there are two callsites with different arguments, we
   * prefer defined constants rather than x-raying through to a string literal, i.e., getName(FOO).
   */
  private static CallSite bestCallsite(List<CallSite> instances) {
    return instances.stream()
        .max(
            Comparator.comparingInt(
                callsite -> callsite.argumentExpression.getKind() == STRING_LITERAL ? 0 : 1))
        .orElseThrow(
            () -> // Impossible, since we got here by groupingBy
            new IllegalArgumentException("No callsites"));
  }
}
