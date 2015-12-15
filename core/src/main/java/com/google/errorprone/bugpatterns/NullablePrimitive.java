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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.methodReturnsNonPrimitiveType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
@BugPattern(
  name = "NullablePrimitive",
  summary = "@Nullable should not be used for primitive types since they cannot be null",
  explanation = "Primitives can never be null.",
  category = JDK,
  severity = WARNING,
  maturity = MATURE
)
public class NullablePrimitive extends BugChecker implements BugChecker.AnnotationTreeMatcher {

  private static final Matcher<Tree> IS_PRIMITIVE_TYPE_MATCHER = isPrimitiveType();
  private static final Matcher<MethodTree> METHOD_RETURNS_NON_PRIMITIVE_TYPE_MATCHER =
      methodReturnsNonPrimitiveType();

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    if (!sym.name.contentEquals("Nullable")) {
      return Description.NO_MATCH;
    }
    Tree annotatedNode = getAnnotatedNode(state);
    Symbol annotatedSymbol = ASTHelpers.getSymbol(annotatedNode);
    if (annotatedSymbol.isConstructor()) {
      // @Nullable constructors get their own check
      return Description.NO_MATCH;
    }
    if (IS_PRIMITIVE_TYPE_MATCHER.matches(annotatedNode, state)) {
      return describeMatch(tree, SuggestedFix.delete(tree));
    } else if (annotatedNode instanceof MethodTree
        && !METHOD_RETURNS_NON_PRIMITIVE_TYPE_MATCHER.matches((MethodTree) annotatedNode, state)) {
      return describeMatch(tree, SuggestedFix.delete(tree));
    }
    return Description.NO_MATCH;
  }

  private static Tree getAnnotatedNode(VisitorState state) {
    return state.getPath().getParentPath().getParentPath().getLeaf();
  }
}
