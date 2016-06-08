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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "BoxedPrimitiveConstructor",
  category = Category.JDK,
  summary = "valueOf or autoboxing provides better time and space performance",
  severity = SeverityLevel.ERROR,
  maturity = MaturityLevel.MATURE
)
public class BoxedPrimitiveConstructor extends BugChecker implements NewClassTreeMatcher {

  private static final Matcher<Tree> TO_STRING =
      toType(ExpressionTree.class, instanceMethod().anyClass().named("toString").withParameters());

  private static final Matcher<Tree> HASH_CODE =
      toType(ExpressionTree.class, instanceMethod().anyClass().named("hashCode").withParameters());

  private static final Matcher<Tree> COMPARE_TO =
      toType(
          ExpressionTree.class,
          instanceMethod().onDescendantOf("java.lang.Comparable").named("compareTo"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (type == null) {
      return Description.NO_MATCH;
    }
    Types types = state.getTypes();
    type = types.unboxedTypeOrType(type);
    if (!type.isPrimitive()) {
      // TODO(cushon): consider handling String also
      return Description.NO_MATCH;
    }
    return describeMatch(tree, buildFix(tree, state, type));
  }

  private Fix buildFix(NewClassTree tree, VisitorState state, Type type) {
    boolean autoboxFix = shouldAutoboxFix(state);
    if (state.getTypes().isSameType(type, state.getSymtab().booleanType)) {
      Object value = literalValue(tree.getArguments().iterator().next());
      if (value instanceof Boolean) {
        return SuggestedFix.replace(tree, literalFix((boolean) value, autoboxFix));
      } else if (value instanceof String) {
        return SuggestedFix.replace(
            tree, literalFix(Boolean.parseBoolean((String) value), autoboxFix));
      }
    }
    JCTree.JCExpression arg = (JCTree.JCExpression) getOnlyElement(tree.getArguments());
    if (autoboxFix && ASTHelpers.getType(arg).isPrimitive()) {
      return SuggestedFix.builder()
          .replace(((JCTree) tree).getStartPosition(), arg.getStartPosition(), "")
          .replace(state.getEndPosition(arg), state.getEndPosition((JCTree) tree), "")
          .build();
    }
    JCTree parent = (JCTree) state.getPath().getParentPath().getParentPath().getLeaf();
    if (TO_STRING.matches(parent, state)) {
      // e.g. new Integer($A).toString() -> String.valueOf($A)
      return SuggestedFix.builder()
          .replace(parent.getStartPosition(), arg.getStartPosition(), "String.valueOf(")
          .replace(state.getEndPosition(arg), state.getEndPosition(parent), ")")
          .build();
    }
    String typeName = state.getSourceForNode(tree.getIdentifier());
    if (HASH_CODE.matches(parent, state)) {
      // e.g. new Integer($A).hashCode() -> Integer.hashCode($A)
      return SuggestedFix.builder()
          .replace(
              parent.getStartPosition(),
              arg.getStartPosition(),
              String.format("%s.hashCode(", typeName))
          .replace(state.getEndPosition(arg), state.getEndPosition(parent), ")")
          .build();
    }
    if (COMPARE_TO.matches(parent, state)
        && ASTHelpers.getReceiver((ExpressionTree) parent).equals(tree)) {
      JCMethodInvocation compareTo = (JCMethodInvocation) parent;
      // e.g. new Integer($A).compareTo($B) -> Integer.compare($A, $B)
      JCTree.JCExpression rhs = getOnlyElement(compareTo.getArguments());
      return SuggestedFix.builder()
          .replace(
              compareTo.getStartPosition(),
              arg.getStartPosition(),
              String.format("%s.compare(", typeName))
          .replace(state.getEndPosition(arg), rhs.getStartPosition(), ", ")
          .replace(state.getEndPosition(rhs), state.getEndPosition(compareTo), ")")
          .build();
    }
    return SuggestedFix.replace(
        ((JCTree) tree).getStartPosition(),
        arg.getStartPosition(),
        String.format("%s.valueOf(", typeName));
  }

  private boolean shouldAutoboxFix(VisitorState state) {
    switch (state.getPath().getParentPath().getLeaf().getKind()) {
      case METHOD_INVOCATION:
        // autoboxing a method argument affects overload resolution
        return false;
      case MEMBER_SELECT:
        // can't select members on primitives (e.g. `theInteger.toString()`)
        return false;
      default:
        return true;
    }
  }

  private String literalFix(boolean value, boolean autoboxFix) {
    if (autoboxFix) {
      return value ? "true" : "false";
    }
    return value ? "Boolean.TRUE" : "Boolean.FALSE";
  }

  private Object literalValue(Tree arg) {
    if (!(arg instanceof LiteralTree)) {
      return null;
    }
    return ((LiteralTree) arg).getValue();
  }
}
