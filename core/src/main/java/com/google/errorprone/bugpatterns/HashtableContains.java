/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "HashtableContains",
  summary = "contains() is a legacy method that is equivalent to containsValue()",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class HashtableContains extends BugChecker implements MethodInvocationTreeMatcher {

  static final Matcher<ExpressionTree> CONTAINS_MATCHER =
      anyOf(
          instanceMethod().onDescendantOf(Hashtable.class.getName()).named("contains"),
          instanceMethod().onDescendantOf(ConcurrentHashMap.class.getName()).named("contains"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CONTAINS_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Description.Builder result = buildDescription(tree);

    // If the collection is not raw, try to figure out if the argument looks like a key
    // or a value.
    List<Type> tyargs = ASTHelpers.getReceiverType(tree).getTypeArguments();
    if (tyargs.size() == 2) {
      // map capture variables to their bounds, e.g. `? extends Number` -> `Number`
      Types types = state.getTypes();
      Type key = ASTHelpers.getUpperBound(tyargs.get(0), types);
      Type value = ASTHelpers.getUpperBound(tyargs.get(1), types);
      Type arg = ASTHelpers.getType(Iterables.getOnlyElement(tree.getArguments()));
      boolean valueShaped = types.isAssignable(arg, value);
      boolean keyShaped = types.isAssignable(arg, key);

      if (keyShaped && !valueShaped) {
        // definitely a key
        result.addFix(replaceMethodName(tree, state, "containsKey"));
        result.setMessage(
            String.format(
                "contains() is a legacy method that is equivalent to containsValue(), but the "
                    + "argument type '%s' looks like a key",
                key));
      } else if (valueShaped && !keyShaped) {
        // definitely a value
        result.addFix(replaceMethodName(tree, state, "containsValue"));
      } else if (valueShaped && keyShaped) {
        // ambiguous
        result.addFix(replaceMethodName(tree, state, "containsValue"));
        result.addFix(replaceMethodName(tree, state, "containsKey"));
        result.setMessage(
            String.format(
                "contains() is a legacy method that is equivalent to containsValue(), but the "
                    + "argument type '%s' could be a key or a value",
                key));
      } else {
        // this shouldn't have compiled!
        throw new AssertionError(
            String.format(
                "unexpected argument to contains(): key: %s, value: %s, argument: %s",
                key, value, arg));
      }
    } else {
      result.addFix(replaceMethodName(tree, state, "containsValue"));
    }

    return result.build();
  }

  private Fix replaceMethodName(MethodInvocationTree tree, VisitorState state, String newName) {
    String source = state.getSourceForNode((JCTree) tree.getMethodSelect());
    int idx = source.lastIndexOf("contains");
    String replacement =
        source.substring(0, idx) + newName + source.substring(idx + "contains".length());
    Fix fix = SuggestedFix.replace(tree.getMethodSelect(), replacement);
    return fix;
  }
}
