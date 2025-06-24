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
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.AnnotationNames.FORMAT_METHOD_ANNOTATION;
import static java.util.stream.Stream.empty;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.stream.Stream;
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

  private static final Matcher<ExpressionTree> STRING_FORMAT =
      staticMethod().onClass("java.lang.String").named("format");
  private static final Matcher<ExpressionTree> FORMATTED =
      instanceMethod().onExactClass("java.lang.String").named("formatted");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    VarSymbol formatString;
    VarSymbol formatArgs;
    if (STRING_FORMAT.matches(tree, state)) {
      if (tree.getArguments().size() != 2) {
        return NO_MATCH;
      }
      formatString = asSymbol(tree.getArguments().get(0));
      formatArgs = asSymbol(tree.getArguments().get(1));
    } else if (FORMATTED.matches(tree, state)) {
      if (tree.getArguments().size() != 1) {
        return NO_MATCH;
      }
      formatString = asSymbol(getReceiver(tree));
      formatArgs = asSymbol(tree.getArguments().get(0));
    } else {
      return NO_MATCH;
    }
    if (formatString == null || formatArgs == null) {
      return NO_MATCH;
    }

    return stream(state.getPath())
        .flatMap(
            node -> {
              if (!(node instanceof MethodTree methodTree)) {
                return empty();
              }
              if (!getSymbol(methodTree).isVarArgs()
                  || hasAnnotation(methodTree, FORMAT_METHOD_ANNOTATION, state)) {
                return empty();
              }
              List<? extends VariableTree> enclosingParameters = methodTree.getParameters();
              VariableTree formatParameter =
                  findParameterWithSymbol(enclosingParameters, formatString);
              VariableTree argumentsParameter =
                  findParameterWithSymbol(enclosingParameters, formatArgs);
              if (formatParameter == null || argumentsParameter == null) {
                return empty();
              }
              if (!argumentsParameter.equals(getLast(enclosingParameters))) {
                return empty();
              }
              // We can only generate a fix if the format string is the penultimate parameter.
              boolean fixable =
                  formatParameter.equals(enclosingParameters.get(enclosingParameters.size() - 2));
              return Stream.of(
                  buildDescription(methodTree)
                      .setMessage(fixable ? message() : (message() + REORDER))
                      .build());
            })
        .findFirst()
        .orElse(NO_MATCH);
  }

  private static VariableTree findParameterWithSymbol(
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
