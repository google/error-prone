/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.code.Type;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.type.TypeKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "RandomCast",
    summary =
        "Casting a random number in the range [0.0, 1.0) to an integer or long always results"
            + " in 0.",
    severity = ERROR)
public class RandomCast extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      anyOf(
          instanceMethod().onExactClass("java.util.Random").namedAnyOf("nextFloat", "nextDouble"),
          staticMethod().onClass("java.lang.Math").named("random"));

  private static final Set<TypeKind> INTEGRAL = EnumSet.of(TypeKind.LONG, TypeKind.INT);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof TypeCastTree)) {
      return NO_MATCH;
    }
    if (!((TypeCastTree) parent).getExpression().equals(tree)) {
      return NO_MATCH;
    }
    Type type = ASTHelpers.getType(parent);
    if (type == null || !INTEGRAL.contains(type.getKind())) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }
}
