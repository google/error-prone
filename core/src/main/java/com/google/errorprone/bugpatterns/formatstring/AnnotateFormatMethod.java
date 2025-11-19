/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.AnnotationNames.FORMAT_METHOD_ANNOTATION;
import static com.google.errorprone.util.AnnotationNames.FORMAT_STRING_ANNOTATION;
import static com.google.errorprone.util.AnnotationNames.LENIENT_FORMAT_STRING_ANNOTATION;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "This method uses a pair of parameters as a format string and its arguments, but the"
            + " enclosing method wasn't annotated. Doing so gives compile-time rather"
            + " than run-time protection against malformed format strings.",
    tags = FRAGILE_CODE,
    severity = WARNING)
public final class AnnotateFormatMethod extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String REORDER =
      " (The parameters of this method would need to be reordered to make the format string and "
          + "arguments the final parameters before the @FormatMethod annotation can be used.)";

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    FormatMethodArguments args = getFormatMethodArguments(tree, state);
    if (args == null) {
      return NO_MATCH;
    }
    if (args.arguments().size() < 2) {
      return NO_MATCH;
    }
    VarSymbol formatString = asSymbol(args.arguments().get(0));
    if (formatString == null) {
      return NO_MATCH;
    }
    for (Tree enclosing : state.getPath()) {
      if (enclosing instanceof MethodTree methodTree) {
        Description description = matchEnclosingMethod(state, methodTree, formatString, args);
        if (description != NO_MATCH) {
          return description;
        }
      }
    }
    return NO_MATCH;
  }

  private static @Nullable FormatMethodArguments getFormatMethodArguments(
      MethodInvocationTree tree, VisitorState state) {
    ImmutableList<ExpressionTree> args = FormatStringUtils.formatMethodArguments(tree, state);
    if (!args.isEmpty()) {
      return new FormatMethodArguments(false, args);
    }
    int index = LenientFormatStringUtils.getLenientFormatStringPosition(tree, state);
    if (index != -1) {
      return new FormatMethodArguments(
          true,
          ImmutableList.copyOf(tree.getArguments().subList(index, tree.getArguments().size())));
    }
    return null;
  }

  private Description matchEnclosingMethod(
      VisitorState state, Tree node, VarSymbol formatString, FormatMethodArguments args) {
    if (!(node instanceof MethodTree methodTree)) {
      return NO_MATCH;
    }
    if (hasAnnotation(methodTree, FORMAT_METHOD_ANNOTATION, state)) {
      return NO_MATCH;
    }
    List<? extends VariableTree> enclosingParameters = methodTree.getParameters();
    VariableTree formatParameter = findParameterWithSymbol(enclosingParameters, formatString);
    if (formatParameter == null) {
      return NO_MATCH;
    }
    if (hasAnnotation(formatParameter, FORMAT_STRING_ANNOTATION, state)
        || hasAnnotation(formatParameter, LENIENT_FORMAT_STRING_ANNOTATION, state)) {
      return NO_MATCH;
    }
    if (args.lenient()) {
      return handleLenient(state, args.arguments(), methodTree, formatParameter);
    }
    if (!getSymbol(methodTree).isVarArgs()) {
      return NO_MATCH;
    }
    VarSymbol formatArgs = asSymbol(args.arguments().get(1));
    if (formatArgs == null) {
      return NO_MATCH;
    }
    VariableTree argumentsParameter = findParameterWithSymbol(enclosingParameters, formatArgs);
    if (argumentsParameter == null) {
      return NO_MATCH;
    }
    if (!argumentsParameter.equals(getLast(enclosingParameters))) {
      return NO_MATCH;
    }
    // We can only generate a fix if the format string is the penultimate parameter.
    boolean fixable =
        formatParameter.equals(enclosingParameters.get(enclosingParameters.size() - 2));
    return buildDescription(methodTree)
        .setMessage(fixable ? message() : (message() + REORDER))
        .build();
  }

  private Description handleLenient(
      VisitorState state,
      List<ExpressionTree> args,
      MethodTree methodTree,
      VariableTree formatParameter) {
    int formatParameterIndex = methodTree.getParameters().indexOf(formatParameter);
    if (args.size() != methodTree.getParameters().size() - formatParameterIndex) {
      return NO_MATCH;
    }
    if (args.size() == 1) {
      return NO_MATCH;
    }
    // Check that all the parameters after the format string are passed through in order.
    for (int i = 1; i < args.size(); i++) {
      if (!(getSymbol(args.get(i)) instanceof VarSymbol vs)
          || !vs.equals(getSymbol(methodTree.getParameters().get(formatParameterIndex + i)))) {
        return NO_MATCH;
      }
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    var lenientFormatString =
        SuggestedFixes.qualifyType(state, fix, LENIENT_FORMAT_STRING_ANNOTATION);
    fix.prefixWith(formatParameter, "@" + lenientFormatString + " ");
    return describeMatch(methodTree, fix.build());
  }

  private record FormatMethodArguments(boolean lenient, ImmutableList<ExpressionTree> arguments) {}

  private static @Nullable VariableTree findParameterWithSymbol(
      List<? extends VariableTree> parameters, Symbol symbol) {
    return parameters.stream()
        .filter(parameter -> symbol.equals(getSymbol(parameter)))
        .collect(toOptional())
        .orElse(null);
  }

  private static @Nullable VarSymbol asSymbol(ExpressionTree tree) {
    return getSymbol(tree) instanceof VarSymbol varSymbol ? varSymbol : null;
  }
}
