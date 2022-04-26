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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.function.Predicate;

/**
 * Checker to prevent usages of comparison methods where both the operands undergo lossy widening.
 *
 * @author awturner@google.com (Andy Turner)
 */
@BugPattern(
    summary = "Using an unnecessarily-wide comparison method can lead to lossy comparison",
    explanation =
        "Implicit widening conversions when comparing two primitives with methods like"
            + " Float.compare can lead to lossy comparison. For example,"
            + " `Float.compare(Integer.MAX_VALUE, Integer.MAX_VALUE - 1) == 0`. Use a compare"
            + " method with non-lossy conversion, or ideally no conversion if possible.",
    severity = ERROR)
public class LossyPrimitiveCompare extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> COMPARE_MATCHER =
      staticMethod().onClassAny("java.lang.Float", "java.lang.Double").named("compare");

  private static final Matcher<ExpressionTree> FLOAT_COMPARE_MATCHER =
      staticMethod().onClass("java.lang.Float").named("compare");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!COMPARE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Types types = state.getTypes();
    Symtab symtab = state.getSymtab();
    ImmutableList<Type> argTypes =
        tree.getArguments().stream().map(ASTHelpers::getType).collect(toImmutableList());
    Predicate<Type> argsAreConvertible =
        type -> argTypes.stream().allMatch(t -> types.isConvertible(t, type));

    if (argsAreConvertible.test(symtab.byteType)
        || argsAreConvertible.test(symtab.charType)
        || argsAreConvertible.test(symtab.shortType)) {
      // These types can be converted to float and double without loss.
    } else if (argsAreConvertible.test(symtab.intType)) {
      // Only need to fix in the case of Float.compare, because int can be converted to double
      // without loss.
      if (FLOAT_COMPARE_MATCHER.matches(tree, state)) {
        return describeMatch(tree, SuggestedFix.replace(tree.getMethodSelect(), "Integer.compare"));
      }
    } else if (argsAreConvertible.test(symtab.longType)) {
      return describeMatch(tree, SuggestedFix.replace(tree.getMethodSelect(), "Long.compare"));
    }

    return Description.NO_MATCH;
  }
}
