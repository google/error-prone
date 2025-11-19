/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.AnnotationNames.FORMAT_METHOD_ANNOTATION;
import static com.google.errorprone.util.AnnotationNames.FORMAT_STRING_ANNOTATION;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** {@link BugChecker} to assert validity of methods calls with {@link FormatString} annotations. */
@BugPattern(
    name = "FormatStringAnnotation",
    summary = "Invalid format string passed to formatting method.",
    severity = ERROR)
public final class FormatStringAnnotationChecker extends BugChecker
    implements MethodInvocationTreeMatcher, MethodTreeMatcher, NewClassTreeMatcher {

  /**
   * Matches a method or constructor invocation. The input symbol should match the invoked method or
   * constructor and the args should be the parameters in the invocation.
   */
  private Description matchInvocation(
      ExpressionTree tree,
      MethodSymbol symbol,
      List<? extends ExpressionTree> args,
      VisitorState state) {
    ImmutableList<ExpressionTree> formatArgs =
        FormatStringUtils.formatMethodAnnotationArguments(tree, symbol, args, state);
    if (formatArgs.isEmpty()) {
      return NO_MATCH;
    }
    FormatStringValidation.ValidationResult result =
        StrictFormatStringValidation.validate(
            formatArgs.get(0), formatArgs.subList(1, formatArgs.size()), state);
    if (result != null) {
      return buildDescription(tree).setMessage(result.message()).build();
    } else {
      return Description.NO_MATCH;
    }
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchInvocation(tree, getSymbol(tree), tree.getArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchInvocation(tree, getSymbol(tree), tree.getArguments(), state);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Type stringType = state.getSymtab().stringType;

    boolean isFormatMethod = hasAnnotation(getSymbol(tree), FORMAT_METHOD_ANNOTATION, state);
    boolean foundFormatString = false;
    boolean foundString = false;
    for (VariableTree param : tree.getParameters()) {
      VarSymbol paramSymbol = getSymbol(param);
      boolean isStringParam = isSameType(paramSymbol.type, stringType, state);

      if (isStringParam) {
        foundString = true;
      }

      if (hasAnnotation(paramSymbol, FORMAT_STRING_ANNOTATION, state)) {
        if (!isFormatMethod) {
          return buildDescription(tree)
              .setMessage(
                  "A parameter can only be annotated @FormatString in a method annotated "
                      + "@FormatMethod: "
                      + state.getSourceForNode(param))
              .build();
        }
        if (!isStringParam) {
          return buildDescription(param)
              .setMessage("Only strings can be annotated @FormatString.")
              .build();
        }
        if (foundFormatString) {
          return buildDescription(tree)
              .setMessage("A method cannot have more than one @FormatString parameter.")
              .build();
        }
        foundFormatString = true;
      }
    }

    if (isFormatMethod && !foundString) {
      return buildDescription(tree)
          .setMessage("An @FormatMethod must contain at least one String parameter.")
          .build();
    }

    return Description.NO_MATCH;
  }
}
