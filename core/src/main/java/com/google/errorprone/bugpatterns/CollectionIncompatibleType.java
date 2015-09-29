/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Checker for calling Object-accepting methods with types that don't match the type arguments of
 * their container types.  Currently this checker detects problems with the following methods on
 * all their subtypes and subinterfaces:
 * <ul>
 * <li>{@link Collection#contains}
 * <li>{@link Collection#remove}
 * <li>{@link List#indexOf}
 * <li>{@link List#lastIndexOf}
 * <li>{@link Map#get}
 * <li>{@link Map#containsKey}
 * <li>{@link Map#remove}
 * <li>{@link Map#containsValue}
 * </ul>
 */
@BugPattern(
  name = "CollectionIncompatibleType",
  summary = "Incompatible type as argument to Object-accepting Java collections method",
  category = JDK,
  maturity = EXPERIMENTAL,
  severity = WARNING
)
public class CollectionIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  /* TODO(eaftan):
   * 1) Add new methods.  The list is in Issue 106.  It might be easier to do it incrementally.
   * 2) Consider whether there is a subset of these that can/should be errors rather than warnings.
   * 3) Bump maturity to MATURE.
   */

  private static final Iterable<MatcherWithTypeInfo> MATCHERS =
      Arrays.asList(
          new MatcherWithTypeInfo("java.util.Collection", "contains(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.Collection", "remove(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.List", "indexOf(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.List", "lastIndexOf(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.Map", "get(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.Map", "containsKey(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.Map", "remove(java.lang.Object)", 0, 0),
          new MatcherWithTypeInfo("java.util.Map", "containsValue(java.lang.Object)", 1, 0));


  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {

    MatchResult result = firstNonNullMatchResult(MATCHERS, tree, state);
    if (result == null) {
      return Description.NO_MATCH;
    }

    Types types = state.getTypes();

    Type methodArgTypeErased = types.erasure(result.methodArgType());
    Type typeArgUpperBoundErased =
        types.erasure(ASTHelpers.getUpperBound(result.typeArgType(), types));
    if (state.getTypes().isAssignable(methodArgTypeErased, typeArgUpperBoundErased)
        || state.getTypes().isAssignable(typeArgUpperBoundErased, methodArgTypeErased)) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "Argument '%s' should not be passed to this method; its type %s is not compatible "
                    + "with its collection's type argument %s",
                result.methodArg(),
                result.methodArgType(),
                result.typeArgType()))
        .build();
  }

  @AutoValue
  abstract static class MatchResult {
    public abstract ExpressionTree methodArg();
    public abstract Type methodArgType();
    public abstract Type typeArgType();

    public static MatchResult create(
        ExpressionTree methodArg, Type methodArgType, Type typeArgType) {
      return new AutoValue_CollectionIncompatibleType_MatchResult(
          methodArg, methodArgType, typeArgType);
    }
  }

  /**
   * Wraps a method invocation matcher with extra information about the method matched.  Extracts
   * the relevant method argument and its type, and the type of the relevant type argument, and
   * returns those along with enough information to produce a helpful error message.
   */
  private static class MatcherWithTypeInfo {

    private final Matcher<ExpressionTree> methodMatcher;
    private final String typeName;
    private final int typeArgIndex;
    private final int methodArgIndex;

    /**
     * @param typeName The fully-qualified name of the type whose descendants to match on
     * @param signature The signature of the method to match on
     * @param typeArgIndex The index of the type argument that should match the method argument
     * @param methodArgIndex The index of the method argument that should match the type argument
     */
    public MatcherWithTypeInfo(
        String typeName, String signature, int typeArgIndex, int methodArgIndex) {
      this.methodMatcher = instanceMethod().onDescendantOf(typeName).withSignature(signature);
      this.typeName = typeName;
      this.typeArgIndex = typeArgIndex;
      this.methodArgIndex = methodArgIndex;
    }

    @Nullable
    public MatchResult matches(MethodInvocationTree tree, VisitorState state) {
      if (!methodMatcher.matches(tree, state)) {
        return null;
      }

      Types types = state.getTypes();

      // Find instantiated parameterized type for the receiver expression that matches the
      // owner of the method we were looking for to handle the case when a subtype has different
      // type arguments than the expected type.  For example, ClassToInstanceMap<T> implements
      // Map<Class<? extends T>, T>.
      Type collectionType =
          types.asSuper(ASTHelpers.getReceiverType(tree), state.getSymbolFromString(typeName));
      com.sun.tools.javac.util.List<Type> tyargs = collectionType.getTypeArguments();
      if (tyargs.size() <= typeArgIndex) {
        // Collection is raw, nothing we can do.
        return null;
      }

      ExpressionTree methodArg = Iterables.get(tree.getArguments(), methodArgIndex);
      Type methodArgType = ASTHelpers.getType(methodArg);
      Type typeArgType = tyargs.get(typeArgIndex);
      return MatchResult.create(methodArg, methodArgType, typeArgType);
    }
  }

  @Nullable
  private static MatchResult firstNonNullMatchResult(
      Iterable<MatcherWithTypeInfo> matchers, MethodInvocationTree tree, VisitorState state) {
    for (MatcherWithTypeInfo matcher : matchers) {
      MatchResult result = matcher.matches(tree, state);
      if (result != null) {
        return result;
      }
    }

    return null;
  }
}
