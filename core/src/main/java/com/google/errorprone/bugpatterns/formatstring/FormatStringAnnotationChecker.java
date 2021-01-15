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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
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
    if (!ASTHelpers.hasAnnotation(symbol, FormatMethod.class, state)) {
      return Description.NO_MATCH;
    }

    int formatString = formatStringIndex(symbol, state);
    if (formatString == -1) {
      // will be an error at call site
      return NO_MATCH;
    }

    FormatStringValidation.ValidationResult result =
        StrictFormatStringValidation.validate(
            args.get(formatString), args.subList(formatString + 1, args.size()), state);

    if (result != null) {
      return buildDescription(tree).setMessage(result.message()).build();
    } else {
      return Description.NO_MATCH;
    }
  }

  private static int formatStringIndex(MethodSymbol symbol, VisitorState state) {
    Type stringType = state.getSymtab().stringType;
    List<VarSymbol> params = symbol.getParameters();
    int firstStringIndex = -1;
    for (int i = 0; i < params.size(); i++) {
      VarSymbol param = params.get(i);
      if (ASTHelpers.hasAnnotation(param, FormatString.class, state)) {
        return i;
      }
      if (firstStringIndex < 0 && ASTHelpers.isSameType(param.type, stringType, state)) {
        firstStringIndex = i;
      }
    }
    return firstStringIndex;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchInvocation(tree, ASTHelpers.getSymbol(tree), tree.getArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchInvocation(tree, ASTHelpers.getSymbol(tree), tree.getArguments(), state);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Type stringType = state.getSymtab().stringType;

    boolean isFormatMethod =
        ASTHelpers.hasAnnotation(ASTHelpers.getSymbol(tree), FormatMethod.class, state);
    boolean foundFormatString = false;
    boolean foundString = false;
    for (VariableTree param : tree.getParameters()) {
      VarSymbol paramSymbol = ASTHelpers.getSymbol(param);
      boolean isStringParam = ASTHelpers.isSameType(paramSymbol.type, stringType, state);

      if (isStringParam) {
        foundString = true;
      }

      if (ASTHelpers.hasAnnotation(paramSymbol, FormatString.class, state)) {
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
