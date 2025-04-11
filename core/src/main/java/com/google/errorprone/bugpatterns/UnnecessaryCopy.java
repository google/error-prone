/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.targetType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashMap;
import java.util.Map;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "This collection is already immutable (just not ImmutableList/ImmutableMap); copying it is"
            + " unnecessary.",
    severity = WARNING)
public final class UnnecessaryCopy extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Matcher<ExpressionTree> IMMUTABLE_COPY =
      staticMethod()
          .onClassAny(
              "com.google.common.collect.ImmutableList", "com.google.common.collect.ImmutableMap")
          .named("copyOf");

  /** Methods that we know return an immutable collection (just not Immutable). */
  private static final Matcher<ExpressionTree> PROTO_GETTER =
      instanceMethod().onDescendantOf("com.google.protobuf.MessageLite");

  private static final Supplier<Type> MAP_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString("java.util.Map"));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Map<VarSymbol, Offender> suspiciousVariables = new HashMap<>();
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree mit, Void unused) {
        if (IMMUTABLE_COPY.matches(mit, state)
            && mit.getArguments().size() == 1
            && PROTO_GETTER.matches(mit.getArguments().get(0), state)) {
          var targetType = targetType(state.withPath(getCurrentPath()));
          if (targetType != null && isSuperType(targetType.type(), state)) {
            state.reportMatch(describe(mit, state));
          } else {
            if (getCurrentPath().getParentPath().getLeaf() instanceof VariableTree vt
                && vt.getInitializer() == mit) {
              suspiciousVariables.put(getSymbol(vt), new Offender(vt, mit));
            }
          }
        }
        return super.visitMethodInvocation(mit, null);
      }
    }.scan(tree, null);

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree id, Void unused) {
        var symbol = getSymbol(id);
        if (suspiciousVariables.containsKey(symbol)) {
          var targetType = targetType(state.withPath(getCurrentPath()));
          if (targetType != null && !isSuperType(targetType.type(), state)) {
            suspiciousVariables.remove(symbol);
          }
        }
        return super.visitIdentifier(id, null);
      }
    }.scan(tree, null);
    for (var offender : suspiciousVariables.values()) {
      state.reportMatch(describe(offender.variableTree, offender.methodInvocationTree, state));
    }
    return NO_MATCH;
  }

  private static boolean isSuperType(Type type, VisitorState state) {
    var erased = state.getTypes().erasure(type);
    return state.getTypes().isSuperType(erased, state.getSymtab().listType)
        || state.getTypes().isSuperType(erased, MAP_TYPE.get(state));
  }

  private Description describe(MethodInvocationTree tree, VisitorState state) {
    return describeMatch(
        tree, SuggestedFix.replace(tree, state.getSourceForNode(tree.getArguments().get(0))));
  }

  private Description describe(
      VariableTree variableTree, MethodInvocationTree tree, VisitorState state) {
    var fix =
        SuggestedFix.builder().replace(tree, state.getSourceForNode(tree.getArguments().get(0)));
    if (!ASTHelpers.hasImplicitType(variableTree, state)) {
      var simpleName =
          SuggestedFixes.qualifyType(state, fix, replacementTypeName(variableTree, state));
      if (variableTree.getType() instanceof ParameterizedTypeTree ptt) {
        fix.replace(ptt.getType(), simpleName);
      } else {
        fix.replace(variableTree.getType(), simpleName);
      }
    }
    return describeMatch(tree, fix.build());
  }

  private static String replacementTypeName(VariableTree variableTree, VisitorState state) {
    if (isSubtype(getType(variableTree.getType()), state.getSymtab().listType, state)) {
      return "java.util.List";
    }
    if (isSubtype(getType(variableTree.getType()), MAP_TYPE.get(state), state)) {
      return "java.util.Map";
    }
    throw new AssertionError("Unknown type");
  }

  /** A suspicious {@link VariableTree} and the {@code copyOf} call used to initialise it. */
  private record Offender(VariableTree variableTree, MethodInvocationTree methodInvocationTree) {}
}
