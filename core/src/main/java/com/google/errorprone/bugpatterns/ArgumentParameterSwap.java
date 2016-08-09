/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
/**
 * This checks the similarity between the arguments and parameters, currently checking if terms have
 * identical names but the arguments are swapped.
 *
 * @author yulissa@google.com (Yulissa Arroyo-Paredes)
 */
@BugPattern(
  name = "ArgumentParameterSwap",
  summary = "The argument and parameter names do not match exactly.",
  category = JDK,
  severity = ERROR,
  maturity = MATURE
)
public class ArgumentParameterSwap extends BugChecker
    implements NewClassTreeMatcher, MethodInvocationTreeMatcher {
  public static final List<String> BLACK_LIST =
      ImmutableList.of("message", "counter", "index", "object", "value", "item", "key");

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    List<VarSymbol> paramList = symbol.getParameters();
    List<ExpressionTree> argList = new ArrayList<ExpressionTree>(tree.getArguments());

    if (evaluateNames(argList, paramList)) {
      return reportMatch(tree, state, argList, paramList);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    List<VarSymbol> paramList = symbol.params;
    List<ExpressionTree> argList = new ArrayList<ExpressionTree>(tree.getArguments());

    if (paramList == null) {
      return Description.NO_MATCH;
    }
    if (evaluateNames(argList, paramList)) {
      return reportMatch(tree, state, argList, paramList);
    }
    return Description.NO_MATCH;
  }

  /**
   * Checks if the current argument and parameters have identical names and are in the same
   * location.
   *
   * @param argList the list of arguments when the method is invoked, must be the same length as
   *     paramList.
   * @param paramList the list of parameters.
   * @return true if the names were the same but not in the correct order.Otherwise false for all
   *     other instances, if the names were in the expected location, if all the params are less
   *     than 4 characters or are part of the blacklist, if even one name isn't exactly the same as
   *     the parameter (or isn't an identifier). In future versions, there names won't have to be
   *     identical but rather the similarity of the names will be calculated.
   */
  private boolean evaluateNames(List<ExpressionTree> argList, List<VarSymbol> paramList) {
    HashSet<String> paramDetails = new HashSet<>();
    HashSet<String> argDetails = new HashSet<>();

    int paramArgNamesMatch = 0;
    for (int i = 0; i < paramList.size(); i++) {
      VarSymbol param = paramList.get(i);
      String paramName = param.getSimpleName().toString();
      paramDetails.add(paramName);
      ExpressionTree arg = argList.get(i);
      // before arg is turned into a string must be a sure it is an IdentifierTree. If the arg
      // is of type int and the expression 1 + 1 is used that would become "1 + 1".
      // TODO(yulissa): Handle cases where a value isn't an identifier but a swap is present
      // among other argument
      if (!(arg instanceof IdentifierTree)) {
        return false;
      }
      String argName = arg.toString();
      argDetails.add(argName);
      // consider it a match if the parameter name is exactly the same as the current argument,
      // if it is less than 4 characters (ex. i), or if it is part of the black list of common terms
      if (argName.equals(paramName) || paramName.length() <= 4 || BLACK_LIST.contains(paramName)) {
        paramArgNamesMatch++;
      }
    }
    if (paramArgNamesMatch == paramDetails.size()) {
      return false;
    }
    return Sets.difference(paramDetails, argDetails).isEmpty();
  }

  private Description reportMatch(
      Tree tree, VisitorState state, List<ExpressionTree> arguments, List<VarSymbol> parameters) {
    Fix fix =
        SuggestedFix.replace(
            ((JCTree) arguments.get(0)).getStartPosition(),
            state.getEndPosition(Iterables.getLast(arguments)),
            Joiner.on(", ").join(parameters));
    return describeMatch(tree, fix);
  }
}
