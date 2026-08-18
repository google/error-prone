/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethodInvocation;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.isSuper;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Simplifies redundant {@code hashCode()} or {@code Objects.hash()} calls inside {@code
 * Objects.hash()}, and simplifies single-argument calls to {@code Objects.hashCode()}.
 */
@BugPattern(
    summary =
        "Calling hashCode() or Objects.hash() inside Objects.hash() is redundant; use"
            + " Objects.hashCode() for single-argument calls.",
    severity = WARNING)
public final class HashCodeInObjectsHash extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> OBJECTS_HASH =
      staticMethod().onClass("java.util.Objects").named("hash");

  private static final Matcher<ExpressionTree> INSTANCE_HASH_CODE =
      instanceMethod().anyClass().named("hashCode").withNoParameters();

  private static final Matcher<ExpressionTree> STATIC_HASH_CODE =
      anyOf(
          staticMethod().onClass("java.util.Objects").named("hashCode"),
          staticMethod()
              .onClassAny(
                  "java.lang.Boolean",
                  "java.lang.Byte",
                  "java.lang.Character",
                  "java.lang.Short",
                  "java.lang.Integer",
                  "java.lang.Long",
                  "java.lang.Float",
                  "java.lang.Double")
              .named("hashCode"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!OBJECTS_HASH.matches(tree, state)) {
      return NO_MATCH;
    }
    // Skip direct arguments of an outer Objects.hash so the outermost call handles all arguments.
    if (isDirectArgumentOfObjectsHash(state)) {
      return NO_MATCH;
    }
    if (containsArrayType(tree.getArguments(), state)) {
      return NO_MATCH;
    }

    List<@Nullable ImmutableList<String>> replacementsList = new ArrayList<>();
    boolean hasReplacement = false;
    for (ExpressionTree arg : tree.getArguments()) {
      ImmutableList<String> replacements = getReplacements(arg, state);
      replacementsList.add(replacements);
      if (replacements != null) {
        hasReplacement = true;
      }
    }

    boolean resultsInSingleArg =
        tree.getArguments().size() == 1
            && (replacementsList.get(0) == null || replacementsList.get(0).size() == 1);

    // Flag if any argument was simplified, or if Objects.hash was called with a single argument.
    if (!hasReplacement && !resultsInSingleArg) {
      return NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    if (resultsInSingleArg) {
      ExpressionTree arg = tree.getArguments().get(0);
      ImmutableList<String> replacements = replacementsList.get(0);
      String singleArg = replacements != null ? replacements.get(0) : state.getSourceForNode(arg);
      if (tree.getMethodSelect() instanceof MemberSelectTree) {
        fix.merge(renameMethodInvocation(tree, "hashCode", state));
        if (replacements != null) {
          fix.replace(arg, singleArg);
        }
      } else {
        String objects = qualifyType(state, fix, "java.util.Objects");
        fix.replace(tree, objects + ".hashCode(" + singleArg + ")");
      }
    } else {
      for (int i = 0; i < tree.getArguments().size(); i++) {
        ExpressionTree arg = tree.getArguments().get(i);
        ImmutableList<String> replacements = replacementsList.get(i);
        if (replacements != null) {
          fix.replace(arg, String.join(", ", replacements));
        }
      }
    }

    return describeMatch(tree, fix.build());
  }

  /** Checks if this call is a direct argument (modulo parens) of an outer Objects.hash. */
  private static boolean isDirectArgumentOfObjectsHash(VisitorState state) {
    TreePath path = state.getPath().getParentPath();
    while (path != null && path.getLeaf() instanceof ParenthesizedTree) {
      path = path.getParentPath();
    }
    return path != null
        && path.getLeaf() instanceof MethodInvocationTree methodInvocationTree
        && OBJECTS_HASH.matches(methodInvocationTree, state);
  }

  private static boolean containsArrayType(
      List<? extends ExpressionTree> arguments, VisitorState state) {
    return arguments.stream().map(ASTHelpers::getType).anyMatch(state.getTypes()::isArray);
  }

  private static @Nullable ImmutableList<String> getReplacements(
      ExpressionTree tree, VisitorState state) {
    ExpressionTree unwrapped = stripParentheses(tree);
    // x.hashCode() -> x (or 'this' for unqualified calls); preserve super.hashCode()
    if (INSTANCE_HASH_CODE.matches(unwrapped, state)) {
      ExpressionTree receiver = getReceiver(unwrapped);
      if (receiver != null && isSuper(receiver)) {
        return null;
      }
      return ImmutableList.of(receiver == null ? "this" : state.getSourceForNode(receiver));
    }
    if (STATIC_HASH_CODE.matches(unwrapped, state)) {
      MethodInvocationTree invocation = (MethodInvocationTree) unwrapped;
      ExpressionTree innerArg = invocation.getArguments().get(0);
      ImmutableList<String> inner = getReplacements(innerArg, state);
      return inner != null ? inner : ImmutableList.of(state.getSourceForNode(innerArg));
    }
    // Flatten nested Objects.hash(a, b) calls into a list of arguments.
    if (OBJECTS_HASH.matches(unwrapped, state)) {
      MethodInvocationTree invocation = (MethodInvocationTree) unwrapped;
      List<? extends ExpressionTree> arguments = invocation.getArguments();
      if (arguments.isEmpty()) {
        // Arrays.hashCode(new Object[0]) == 1
        return ImmutableList.of("1");
      }
      if (containsArrayType(arguments, state)) {
        return null;
      }
      ImmutableList.Builder<String> result = ImmutableList.builder();
      for (ExpressionTree arg : arguments) {
        ImmutableList<String> inner = getReplacements(arg, state);
        if (inner != null) {
          result.addAll(inner);
        } else {
          result.add(state.getSourceForNode(arg));
        }
      }
      return result.build();
    }
    return null;
  }
}
