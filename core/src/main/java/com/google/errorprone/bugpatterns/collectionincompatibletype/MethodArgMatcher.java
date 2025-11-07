/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Matches an instance method like {@link Collection#contains}, for which we just need to compare
 * the method argument's type to the receiver's type argument. This is the common case.
 */
final class MethodArgMatcher extends AbstractCollectionIncompatibleTypeMatcher {

  private final Matcher<ExpressionTree> methodMatcher;
  private final String typeName;
  private final int typeArgIndex;
  private final int methodArgIndex;

  /**
   * @param typeName The fully-qualified name of the type whose descendants to match on
   * @param typeArgIndex The index of the type argument that should match the method argument
   * @param methodArgIndex The index of the method argument that should match the type argument
   * @param name The name of the method to match on
   * @param firstParam The type of the first parameter of the method
   * @param otherParams The types of any additional parameters of the method
   */
  MethodArgMatcher(
      String typeName,
      int typeArgIndex,
      int methodArgIndex,
      String name,
      String firstParam,
      String... otherParams) {
    this.methodMatcher =
        instanceMethod()
            .onDescendantOf(typeName)
            .named(name)
            .withParameters(firstParam, otherParams);
    this.typeName = typeName;
    this.typeArgIndex = typeArgIndex;
    this.methodArgIndex = methodArgIndex;
  }

  @Override
  Matcher<ExpressionTree> methodMatcher() {
    return methodMatcher;
  }

  @Override
  ExpressionTree extractSourceTree(MethodInvocationTree tree, VisitorState state) {
    return Iterables.get(tree.getArguments(), methodArgIndex);
  }

  @Override
  @Nullable ExpressionTree extractSourceTree(MemberReferenceTree tree, VisitorState state) {
    return tree;
  }

  @Override
  Type extractSourceType(MethodInvocationTree tree, VisitorState state) {
    return getType(extractSourceTree(tree, state));
  }

  @Override
  @Nullable Type extractSourceType(MemberReferenceTree tree, VisitorState state) {
    return state.getTypes().findDescriptorType(getType(tree)).getParameterTypes().getFirst();
  }

  @Override
  Type extractTargetType(MethodInvocationTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        ASTHelpers.getReceiverType(tree),
        state.getSymbolFromString(typeName),
        typeArgIndex,
        state.getTypes());
  }

  @Override
  @Nullable Type extractTargetType(MemberReferenceTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        ASTHelpers.getReceiverType(tree),
        state.getSymbolFromString(typeName),
        typeArgIndex,
        state.getTypes());
  }

  @Override
  Optional<Fix> buildFix(MatchResult result) {
    return Optional.of(SuggestedFix.prefixWith(result.sourceTree(), "(Object) "));
  }

  @Override
  public String message(MatchResult result, String sourceType, String targetType) {
    return String.format(
        "Argument '%s' should not be passed to this method; its type %s is not compatible "
            + "with its collection's type argument %s",
        result.sourceTree(), sourceType, targetType);
  }
}
