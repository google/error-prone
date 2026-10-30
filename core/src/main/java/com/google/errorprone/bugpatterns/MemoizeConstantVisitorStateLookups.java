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

import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimaps;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ClassName;
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
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.util.Name;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Anytime you need to look up a constant value from VisitorState, improve performance by"
            + " creating a cache for it with ClassName or VisitorState.memoize",
    severity = SeverityLevel.WARNING)
public class MemoizeConstantVisitorStateLookups extends BugChecker
    implements CompilationUnitTreeMatcher {

  private static final Matcher<ExpressionTree> CONSTANT_LOOKUP =
      instanceMethod()
          .onExactClass(VisitorState.class.getName())
          .namedAnyOf(
              "getName", "getTypeFromString", "getSymbolFromString", "binaryNameFromClassname")
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
    ImmutableList<CallSite> lookups = findConstantLookups(state);
    if (lookups.isEmpty()) {
      return NO_MATCH;
    }
    ImmutableListMultimap<LookupKey, CallSite> callsByKey =
        Multimaps.index(lookups, CallSite::lookupKey);
    ImmutableListMultimap<String, LookupKey> keysByBaseName =
        Multimaps.index(callsByKey.keySet(), LookupKey::baseConstantName);

    Set<String> usedFieldNames = new HashSet<>();
    for (Tree member : tree.getMembers()) {
      if (member instanceof VariableTree variableTree) {
        usedFieldNames.add(variableTree.getName().toString());
      }
    }

    // addMembers can only be called once per class, so we emit just one fix for all occurrences.
    SuggestedFix.Builder fix = SuggestedFix.builder();
    ImmutableSortedSet.Builder<String> membersToAdd =
        new ImmutableSortedSet.Builder<>(naturalOrder());
    for (String baseName : keysByBaseName.keySet()) {
      ImmutableList<LookupKey> keys = keysByBaseName.get(baseName);
      boolean needsKindSuffix = keys.size() > 1;
      for (LookupKey key : keys) {
        ImmutableList<CallSite> instances = callsByKey.get(key);
        String constantName =
            chooseConstantName(baseName, key.kind(), needsKindSuffix, instances, usedFieldNames);
        usedFieldNames.add(constantName);
        membersToAdd.add(memoizeLookup(state, fix, constantName, key.kind(), instances));
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

  private static String chooseConstantName(
      String baseName,
      LookupKind kind,
      boolean needsKindSuffix,
      ImmutableList<CallSite> instances,
      Set<String> usedFieldNames) {
    CallSite prototype = bestCallsite(instances);
    String referencedIdentifier =
        prototype.argumentExpression() instanceof IdentifierTree id
            ? id.getName().toString()
            : null;
    if (!needsKindSuffix
        && !usedFieldNames.contains(baseName)
        && !baseName.equals(referencedIdentifier)) {
      return baseName;
    }
    String withKind = baseName + "_" + kind.name();
    if (!usedFieldNames.contains(withKind) && !withKind.equals(referencedIdentifier)) {
      return withKind;
    }
    for (int i = 2; ; i++) {
      String numbered = withKind + "_" + i;
      if (!usedFieldNames.contains(numbered) && !numbered.equals(referencedIdentifier)) {
        return numbered;
      }
    }
  }

  private enum LookupKind {
    NAME,
    TYPE
  }

  private record LookupKey(LookupKind kind, String normalizedArgument) {
    String baseConstantName() {
      return Ascii.toUpperCase(normalizedArgument).replaceAll("\\W", "_");
    }
  }

  /**
   * @param method The method on VisitorState being called.
   * @param lookupKey The normalized lookup key for grouping equivalent lookups.
   * @param argumentExpression The actual expression with that value: a string literal, or a
   *     constant with such a value.
   * @param entireTree The entire invocation of the VisitorState method.
   */
  private record CallSite(
      Name method,
      LookupKey lookupKey,
      ExpressionTree argumentExpression,
      MethodInvocationTree entireTree) {}

  private ImmutableList<CallSite> findConstantLookups(VisitorState state) {
    ImmutableList.Builder<CallSite> result = ImmutableList.builder();
    new SuppressibleTreePathScanner<Void, Void>(state) {
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
        ExpressionTree argumentExpr = tree.getArguments().getFirst();
        String argumentValue = ASTHelpers.constValue(argumentExpr, String.class);
        if (argumentValue == null
            || !(tree.getMethodSelect() instanceof JCFieldAccess fieldAccess)) {
          return;
        }
        Name method = fieldAccess.name;
        LookupKey lookupKey;
        if (method.contentEquals("getName")) {
          lookupKey = new LookupKey(LookupKind.NAME, argumentValue);
        } else {
          try {
            lookupKey = new LookupKey(LookupKind.TYPE, ClassName.of(argumentValue).toString());
          } catch (IllegalArgumentException e) {
            return;
          }
        }
        result.add(new CallSite(method, lookupKey, argumentExpr, tree));
      }
    }.scan(state.getPath(), null);
    return result.build();
  }

  /**
   * Adds to {@code fix} the changes necessary to memoize all the callsites in {@code instances},
   * and returns the new constant declaration to which the fixed callsites will refer.
   */
  private static String memoizeLookup(
      VisitorState state,
      SuggestedFix.Builder fix,
      String newConstantName,
      LookupKind lookupKind,
      ImmutableList<CallSite> instances) {

    CallSite prototype = bestCallsite(instances);

    return switch (lookupKind) {
      case NAME -> {
        for (CallSite instance : instances) {
          var visitorStateExpr = getReceiver(instance.entireTree);
          fix.replace(
              instance.entireTree,
              "%s.get(%s)".formatted(newConstantName, state.getSourceForNode(visitorStateExpr)));
        }
        String rhs =
            "%s.memoize(state -> state.getName(%s))"
                .formatted(
                    qualifyType(state, fix, VisitorState.class.getCanonicalName()),
                    state.getSourceForNode(prototype.argumentExpression));
        yield "private static final %s<%s> %s = %s;"
            .formatted(
                qualifyType(state, fix, Supplier.class.getCanonicalName()),
                qualifyType(state, fix, Name.class.getCanonicalName()),
                newConstantName,
                rhs);
      }
      case TYPE -> {
        for (CallSite instance : instances) {
          var visitorStateExpr = getReceiver(instance.entireTree);
          String accessor =
              switch (instance.method.toString()) {
                case "getTypeFromString" -> "type";
                case "getSymbolFromString" -> "symbol";
                case "binaryNameFromClassname" -> "binaryName";
                default -> throw new AssertionError("Unexpected method: " + instance.method);
              };
          fix.replace(
              instance.entireTree,
              "%s.%s(%s)"
                  .formatted(newConstantName, accessor, state.getSourceForNode(visitorStateExpr)));
        }
        String classNameType = qualifyType(state, fix, ClassName.class.getCanonicalName());
        yield "private static final %s %s = %s.of(%s);"
            .formatted(
                classNameType,
                newConstantName,
                classNameType,
                state.getSourceForNode(prototype.argumentExpression));
      }
    };
  }

  /**
   * Chooses one call site to use for understanding all related callsites. This is used to determine
   * what type is being looked up and what source text to use as the argument to the VisitorState
   * method. The idea is that if someone writes both getName("foo") and getName(FOO), we want to
   * only define one constant for them. In case there are two callsites with different arguments, we
   * prefer defined constants rather than x-raying through to a string literal, i.e., getName(FOO).
   */
  private static CallSite bestCallsite(ImmutableList<CallSite> instances) {
    return instances.stream()
        .max(
            Comparator.comparingInt(
                callsite -> callsite.argumentExpression.getKind() == STRING_LITERAL ? 0 : 1))
        .orElseThrow(
            () -> // Impossible, since we got here by groupingBy
            new IllegalArgumentException("No callsites"));
  }
}
