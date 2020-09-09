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

import com.google.common.base.Ascii;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Table;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "MemoizeConstantVisitorStateLookups",
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
    // row=method, column=argument, value=use sites
    Table<String, String, Set<MethodInvocationTree>> lookups = findConstantLookups(tree, state);

    if (lookups.isEmpty()) {
      return NO_MATCH;
    }

    // addMembers can only be called once per class, so we have to emit a single fix for all
    // occurrences.
    SuggestedFix.Builder fix = SuggestedFix.builder();

    ImmutableSortedSet.Builder<String> membersToAdd =
        new ImmutableSortedSet.Builder<>(Comparator.naturalOrder());
    for (Map.Entry<String, Map<String, Set<MethodInvocationTree>>> lookup :
        lookups.columnMap().entrySet()) {
      String argument = lookup.getKey();
      Map<String, Set<MethodInvocationTree>> usages = lookup.getValue();

      if (usages.size() == 1) {
        // The common case: we have state.foo(argument), and never state.bar(argument), so we can
        // name the constant after just argument.
        Map.Entry<String, Set<MethodInvocationTree>> useSites = usages.entrySet().iterator().next();
        String methodName = useSites.getKey();
        Set<MethodInvocationTree> instances = useSites.getValue();
        memoizeSupplier(
            state, fix, membersToAdd::add, argument, methodName, instances, (name, type) -> name);
      } else {
        // Sadly we have both state.foo(argument) and also state.bar(argument), so we need two
        // constants based on argument and must disambiguate their names.
        for (Map.Entry<String, Set<MethodInvocationTree>> usage : usages.entrySet()) {
          String methodName = usage.getKey();
          Set<MethodInvocationTree> instances = usage.getValue();
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

    SuggestedFixes.addMembers(
            (ClassTree)
                tree.getTypeDecls().stream()
                    .filter(t -> t instanceof ClassTree)
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new IllegalArgumentException(
                                "Compilation unit had no type definitions, but somehow contained"
                                    + " replaceable method invocations")),
            state,
            AdditionPosition.LAST,
            membersToAdd.build())
        .ifPresent(fix::merge);
    return describeMatch(tree, fix.build());
  }

  private static Table<String, String, Set<MethodInvocationTree>> findConstantLookups(
      CompilationUnitTree tree, VisitorState state) {
    Table<String, String, Set<MethodInvocationTree>> lookups = HashBasedTable.create();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (CONSTANT_LOOKUP.matches(tree, state)) {
          String argument = ASTHelpers.constValue(tree.getArguments().get(0), String.class);
          if (argument != null) {
            JCFieldAccess methodSelect = (JCFieldAccess) tree.getMethodSelect();
            String method = methodSelect.name.toString();
            if (!lookups.contains(method, argument)) {
              lookups.put(method, argument, new HashSet<>());
            }
            lookups.get(method, argument).add(tree);
          }
        }
        if (MEMOIZE_CALL.matches(tree, state)) {
          // Don't descend into calls to memoize, because they're already properly memoized!
          return null;
        }
        return super.visitMethodInvocation(tree, null);
      }
    }.scan(tree, null);
    return lookups;
  }

  private void memoizeSupplier(
      VisitorState state,
      SuggestedFix.Builder fix,
      Consumer<String> memberConsumer,
      String argument,
      String methodName,
      Set<MethodInvocationTree> instances,
      BiFunction<String, String, String> namingStrategy) {
    // Arbitrarily choose one call site to determine what type is being looked up.
    MethodSymbol sym = ASTHelpers.getSymbol(instances.iterator().next());
    TypeSymbol returnType = sym.getReturnType().tsym;
    String returnTypeName = returnType.getSimpleName().toString();
    String newConstantPrefix = Ascii.toUpperCase(argument).replaceAll("\\W", "_");
    String newConstantName =
        namingStrategy.apply(newConstantPrefix, Ascii.toUpperCase(returnTypeName));

    memberConsumer.accept(
        defineConstant(
            SuggestedFixes.qualifyType(state, fix, Supplier.class.getCanonicalName()),
            SuggestedFixes.qualifyType(state, fix, VisitorState.class.getCanonicalName()),
            newConstantName,
            methodName,
            argument,
            SuggestedFixes.qualifyType(state, fix, returnType.getQualifiedName().toString())));
    for (MethodInvocationTree instance : instances) {
      ExpressionTree visitorStateExpr = ASTHelpers.getReceiver(instance);
      fix.replace(
          instance,
          String.format("%s.get(%s)", newConstantName, state.getSourceForNode(visitorStateExpr)));
    }
  }

  private static String defineConstant(
      String supplierType,
      String visitorStateType,
      String newConstantName,
      String method,
      String argument,
      String type) {
    return String.format(
        "private static final %s<%s> %s = %s.memoize(state -> state.%s(\"%s\"));",
        supplierType, type, newConstantName, visitorStateType, method, argument);
  }
}
