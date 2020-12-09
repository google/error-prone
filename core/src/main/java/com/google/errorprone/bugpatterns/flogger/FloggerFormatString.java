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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.formatstring.FormatStringValidation;
import com.google.errorprone.bugpatterns.formatstring.FormatStringValidation.ValidationResult;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import edu.umd.cs.findbugs.formatStringChecker.ExtraFormatArgumentsException;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "FloggerFormatString",
    altNames = "FormatString",
    summary = "Invalid printf-style format string",
    severity = ERROR)
public class FloggerFormatString extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> FORMAT_METHOD =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log");

  private static final Matcher<ExpressionTree> WITH_CAUSE =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("withCause");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!FORMAT_METHOD.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getArguments().isEmpty()) {
      return NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    FormatStringValidation.ValidationResult result =
        FormatStringValidation.validate(sym, tree.getArguments(), state);
    if (result == null) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree).setMessage(result.message());
    Fix fix = withCauseFix(result, tree, state);
    if (fix != null) {
      description.addFix(fix);
    }
    return description.build();
  }

  /**
   * If there were more arguments than format specifiers and the last argument is an exception,
   * suggest using {@code withCause(e)} instead of adding a format specifier.
   */
  private Fix withCauseFix(
      ValidationResult result, MethodInvocationTree tree, final VisitorState state) {
    if (!(result.exception() instanceof ExtraFormatArgumentsException)) {
      return null;
    }
    ExtraFormatArgumentsException exception = (ExtraFormatArgumentsException) result.exception();
    if (exception.used >= exception.provided) {
      return null;
    }
    ExpressionTree last = getLast(tree.getArguments());
    if (!ASTHelpers.isSubtype(ASTHelpers.getType(last), state.getSymtab().throwableType, state)) {
      return null;
    }

    // if there's already a call to withCause, don't suggest adding another one
    final boolean[] withCause = {false};
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            if (WITH_CAUSE.matches(tree, state)) {
              withCause[0] = true;
            }
            return super.visitMethodInvocation(tree, null);
          }
        },
        null);
    if (withCause[0]) {
      return null;
    }

    return SuggestedFix.builder()
        .replace(
            state.getEndPosition((JCTree) tree.getArguments().get(tree.getArguments().size() - 2)),
            state.getEndPosition((JCTree) last),
            "")
        .postfixWith(
            ASTHelpers.getReceiver(tree),
            String.format(".withCause(%s)", state.getSourceForNode(last)))
        .build();
  }
}
