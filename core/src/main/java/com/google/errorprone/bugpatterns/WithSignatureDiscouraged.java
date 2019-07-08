/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.stream.Collectors;

/**
 * {@link
 * com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher#withSignature(String)} is
 * discouraged: most usages should use .named and/or .withParameters instead.
 *
 * @author amalloy@google.com (Alan Malloy)
 */
@BugPattern(
    name = "WithSignatureDiscouraged",
    summary = "withSignature is discouraged. Prefer .named and/or .withParameters where possible.",
    providesFix = REQUIRES_HUMAN_ATTENTION,
    severity = WARNING)
public class WithSignatureDiscouraged extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> WITH_SIGNATURE =
      instanceMethod()
          .onExactClass("com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher")
          .named("withSignature")
          .withParameters("java.lang.String");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!WITH_SIGNATURE.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree argument = tree.getArguments().get(0);
    String sig = ASTHelpers.constValue(argument, String.class);
    if (sig == null) {
      // Non-const arguments to withSignature are a bit weird but there's not much we can do.
      return NO_MATCH;
    }
    if (sig.contains("...") || sig.contains("<")) {
      // We don't have a migration strategy to suggest for varargs or generics.
      return NO_MATCH;
    }

    int firstParenIndex = sig.indexOf('(');
    if (firstParenIndex == -1) {
      // .withSignature("toString") => .named("toString")
      return describeMatch(tree, SuggestedFixes.renameMethodInvocation(tree, "named", state));
    }

    // .withSignature("valueOf(java.lang.String,int)")
    //      =>
    // .named("valueOf").withParameters("java.lang.String", "int")
    if (!(tree instanceof JCTree)) {
      // We can't easily compute offsets to replace a whole method chain based on just the public
      // Tree API.
      return NO_MATCH;
    }
    String methodName = sig.substring(0, firstParenIndex);
    String paramList = sig.substring(firstParenIndex + 1, sig.length() - 1);
    return fixWithParameters((JCTree) tree, state, methodName, paramList);
  }

  private Description fixWithParameters(
      JCTree tree, VisitorState state, String methodName, String paramList) {
    ImmutableList<String> paramTypes =
        ImmutableList.copyOf(Splitter.on(',').omitEmptyStrings().split(paramList));
    if (paramTypes.stream().anyMatch(type -> isProbableTypeParameter(type) || isArrayType(type))) {
      // We can't migrate references to type parameters or arrays, because withParameters doesn't
      // handle those.
      return NO_MATCH;
    }

    // A single string representing all the args to withParameters
    String withParamsArgList =
        paramTypes.stream()
            .map(type -> String.format("\"%s\"", type))
            .collect(Collectors.joining(", "));
    int treeStart = tree.getStartPosition();
    String source = state.getSourceForNode(tree);
    if (source == null) {
      // Not clear how this could happen, but if it does we may as well give up.
      return NO_MATCH;
    }
    // Technically too restrictive, but will handle google-java-format'ed code okay.
    int offset = source.indexOf(".withSignature");
    if (offset == -1) {
      return NO_MATCH;
    }
    int startPosition = treeStart + offset;
    int endPosition = state.getEndPosition(tree);
    String replacementText =
        String.format(".named(\"%s\").withParameters(%s)", methodName, withParamsArgList);
    return describeMatch(tree, SuggestedFix.replace(startPosition, endPosition, replacementText));
  }

  private static boolean isArrayType(String type) {
    return type.contains("[");
  }

  private static boolean isProbableTypeParameter(String type) {
    // This is a reasonable heuristic for references to type parameters:
    // they have no . in them, and are not all-lowercase.
    return !type.contains(".") && !type.equals(Ascii.toLowerCase(type));
  }
}
