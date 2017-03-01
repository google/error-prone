/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Resolve;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "LiteralClassName",
  category = JDK,
  summary = "Using Class.forName is unnecessary if the class is available at compile-time.",
  severity = WARNING
)
public class LiteralClassName extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> CLASS_NAME =
      staticMethod().onClass("java.lang.Class").withSignature("forName(java.lang.String)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CLASS_NAME.matches(tree, state)) {
      return NO_MATCH;
    }
    String className = constValue(getOnlyElement(tree.getArguments()), String.class);
    if (className == null) {
      return NO_MATCH;
    }
    if (className.startsWith("[")) {
      // TODO(cushon): consider handling arrays
      return NO_MATCH;
    }
    Type type = state.getTypeFromString(className);
    if (type == null) {
      return NO_MATCH;
    }
    ClassSymbol owner = getSymbol(state.findEnclosing(ClassTree.class));
    Enter enter = Enter.instance(state.context);
    if (!Resolve.instance(state.context).isAccessible(enter.getEnv(owner), type.tsym)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String replaceWith =
        String.format("%s.class", qualifyType(state, fix, state.getTypes().erasure(type)));
    if (state.getPath().getParentPath().getLeaf().getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
      fix.addStaticImport("java.util.Objects.requireNonNull");
      replaceWith = String.format("requireNonNull(%s)", replaceWith);
    }
    fix.replace(tree, replaceWith);
    return describeMatch(tree, fix.build());
  }
}
