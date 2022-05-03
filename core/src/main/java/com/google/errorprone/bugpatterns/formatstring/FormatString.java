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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Invalid printf-style format string", severity = ERROR)
public class FormatString extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> FORMATTED_METHOD =
      instanceMethod().onExactClass("java.lang.String").named("formatted");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<ExpressionTree> args;
    MethodSymbol sym = ASTHelpers.getSymbol(tree);

    if (FORMATTED_METHOD.matches(tree, state)) {
      /*
        Java 15 and greater supports the formatted method on an instance of string. If found
        then use the string value as the pattern and all-of-the arguments and send directly to
        the validate method.
      */
      ExpressionTree receiver = ASTHelpers.getReceiver(tree);
      if (receiver == null) {
        // an unqualified call to 'formatted', possibly inside the definition
        // of java.lang.String
        return Description.NO_MATCH;
      }
      args =
          ImmutableList.<ExpressionTree>builder().add(receiver).addAll(tree.getArguments()).build();

    } else {
      args = FormatStringUtils.formatMethodArguments(tree, state);
    }
    if (args.isEmpty()) {
      return Description.NO_MATCH;
    }
    FormatStringValidation.ValidationResult result =
        FormatStringValidation.validate(sym, args, state);
    if (result == null) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).setMessage(result.message()).build();
  }
}
