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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Detects occurrences of pairs of parameters being passed straight through to {@link String#format}
 * from a method not annotated with {@link FormatMethod}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "AnnotateFormatMethod",
    summary =
        "This method passes a pair of parameters through to String.format, but the enclosing "
            + "method wasn't annotated @FormatMethod. Doing so gives compile-time rather than "
            + "run-time protection against malformed format strings.",
    severity = WARNING,
    tags = FRAGILE_CODE,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class AnnotateFormatMethod extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String REORDER =
      " The parameters of this method would need to be reordered to make the format string and "
          + "arguments the final parameters before the @FormatMethod annotation can be used.";

  private static final Matcher<ExpressionTree> STRING_FORMAT =
      staticMethod().onClass("java.lang.String").named("format");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!STRING_FORMAT.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    if (tree.getArguments().size() != 2) {
      return Description.NO_MATCH;
    }
    VarSymbol formatString = asSymbol(tree.getArguments().get(0));
    VarSymbol formatArgs = asSymbol(tree.getArguments().get(1));
    if (formatString == null || formatArgs == null) {
      return Description.NO_MATCH;
    }
    MethodTree enclosingMethod = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (enclosingMethod == null
        || !ASTHelpers.getSymbol(enclosingMethod).isVarArgs()
        || ASTHelpers.hasAnnotation(enclosingMethod, FormatMethod.class, state)) {
      return Description.NO_MATCH;
    }
    List<? extends VariableTree> enclosingParameters = enclosingMethod.getParameters();
    Optional<? extends VariableTree> formatParameter =
        findParameterWithSymbol(enclosingParameters, formatString);
    Optional<? extends VariableTree> argumentsParameter =
        findParameterWithSymbol(enclosingParameters, formatArgs);
    if (!formatParameter.isPresent() || !argumentsParameter.isPresent()) {
      return Description.NO_MATCH;
    }
    if (!argumentsParameter.get().equals(getLast(enclosingParameters))) {
      return Description.NO_MATCH;
    }
    // We can only generate a fix if the format string is the penultimate parameter.
    boolean fixable =
        formatParameter.get().equals(enclosingParameters.get(enclosingParameters.size() - 2));
    if (fixable) {
      return buildDescription(enclosingMethod)
          .addFix(
              SuggestedFix.builder()
                  .prefixWith(enclosingMethod, "@FormatMethod ")
                  .prefixWith(formatParameter.get(), "@FormatString ")
                  .addImport("com.google.errorprone.annotations.FormatMethod")
                  .addImport("com.google.errorprone.annotations.FormatString")
                  .build())
          .build();
    }
    return buildDescription(enclosingMethod).setMessage(message() + REORDER).build();
  }

  private static Optional<? extends VariableTree> findParameterWithSymbol(
      List<? extends VariableTree> parameters, Symbol symbol) {
    return parameters.stream()
        .filter(parameter -> symbol.equals(ASTHelpers.getSymbol(parameter)))
        .collect(toOptional());
  }

  @Nullable
  private static VarSymbol asSymbol(ExpressionTree tree) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    return symbol instanceof VarSymbol ? (VarSymbol) symbol : null;
  }
}
