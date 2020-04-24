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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import javax.annotation.Nullable;

final class BinopMatcher extends AbstractCollectionIncompatibleTypeMatcher {

  private final Matcher<ExpressionTree> matcher;
  private final String collectionType;

  BinopMatcher(String collectionType, String className, String methodName) {
    this.collectionType = collectionType;
    matcher = Matchers.staticMethod().onClass(className).named(methodName);
  }

  @Override
  Matcher<ExpressionTree> methodMatcher() {
    return matcher;
  }

  @Nullable
  @Override
  Type extractSourceType(MethodInvocationTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        getType(tree.getArguments().get(0)),
        state.getSymbolFromString(collectionType),
        0,
        state.getTypes());
  }

  @Nullable
  @Override
  Type extractSourceType(MemberReferenceTree tree, VisitorState state) {
    Type descriptorType = state.getTypes().findDescriptorType(getType(tree));
    return extractTypeArgAsMemberOfSupertype(
        descriptorType.getParameterTypes().get(0),
        state.getSymbolFromString(collectionType),
        0,
        state.getTypes());
  }

  @Nullable
  @Override
  ExpressionTree extractSourceTree(MethodInvocationTree tree, VisitorState state) {
    return tree.getArguments().get(0);
  }

  @Nullable
  @Override
  ExpressionTree extractSourceTree(MemberReferenceTree tree, VisitorState state) {
    return tree;
  }

  @Nullable
  @Override
  Type extractTargetType(MethodInvocationTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        getType(tree.getArguments().get(1)),
        state.getSymbolFromString(collectionType),
        0,
        state.getTypes());
  }

  @Nullable
  @Override
  Type extractTargetType(MemberReferenceTree tree, VisitorState state) {
    Type descriptorType = state.getTypes().findDescriptorType(getType(tree));
    return extractTypeArgAsMemberOfSupertype(
        descriptorType.getParameterTypes().get(1),
        state.getSymbolFromString(collectionType),
        0,
        state.getTypes());
  }

  @Override
  protected String message(MatchResult result, String sourceType, String targetType) {
    return String.format(
        "Argument '%s' should not be passed to this method; its type %s is not compatible with"
            + " %s",
        result.sourceTree(), sourceType, targetType);
  }
}
