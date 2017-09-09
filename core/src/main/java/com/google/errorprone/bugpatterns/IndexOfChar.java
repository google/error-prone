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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "IndexOfChar",
  category = JDK,
  summary =
      "The first argument to indexOf is a Unicode code point, and the second is the index to start"
          + " the search from",
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class IndexOfChar extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.instanceMethod()
          .onClass(TypePredicates.isExactType(Suppliers.STRING_TYPE))
          .named("indexOf")
          .withParameters("int", "int");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    Symtab syms = state.getSymtab();
    Types types = state.getTypes();
    if (types.isSameType(types.unboxedTypeOrType(getType(arguments.get(0))), syms.intType)
        && types.isSameType(types.unboxedTypeOrType(getType(arguments.get(1))), syms.charType)) {
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .replace(arguments.get(0), state.getSourceForNode(arguments.get(1)))
              .replace(arguments.get(1), state.getSourceForNode(arguments.get(0)))
              .build());
    }
    return NO_MATCH;
  }
}
