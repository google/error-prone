/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.contains;
import static com.google.errorprone.matchers.Matchers.hasArguments;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.toType;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * Static Datastore matchers
 */
public class DatastoreMatchers {
  public static final String QUERY = "com.google.appengine.api.datastore.Query";
  public static final String ENTITY = "com.google.appengine.api.datastore.Entity";

  public static final Matcher<Tree> hasSetKeysOnlyMethodCall =
      contains(
          toType(ExpressionTree.class, instanceMethod().onExactClass(QUERY).named("setKeysOnly")));

  private static final Matcher<ExpressionTree> queryExpr = isSameType(QUERY);
  private static final Matcher<ExpressionTree> entityExpr = isSameType(ENTITY);

  /**
   * Matches on an Entity instance with any method call
   */
  private static final Matcher<ExpressionTree> anyInstanceMethodOnEntity =
      instanceMethod().onExactClass(ENTITY).withAnyName();
  private static final Matcher<Tree> hasInstanceMethodCallOnEntity =
      contains(toType(ExpressionTree.class, anyInstanceMethodOnEntity));

  /**
   * Matches on Query, Entity, or Iterable&lt;Entity&gt; parameters
   */
  private static final Matcher<VariableTree> queryVar = isSameType(QUERY);
  private static final Matcher<VariableTree> entityVar = isSameType(ENTITY);
  // TODO(eaftan): Use type argument to detect Iterable<Entity>.
  private static final Matcher<VariableTree> iterableVar = isSubtypeOf("java.lang.Iterable");
  private static final Matcher<MethodTree> hasParamOfTypeQueryEntityOrIterable =
      methodHasParameters(MatchType.AT_LEAST_ONE, anyOf(queryVar, entityVar, iterableVar));

  /**
   * Matches on prepare(Query) by DatastoreService instance
   */
  private static final Matcher<ExpressionTree> prepareQuery =
      instanceMethod()
          .onExactClass("com.google.appengine.api.datastore.DatastoreService")
          .named("prepare");
  private static final Matcher<Tree> hasPrepareMethodCallOnDatastoreService =
      contains(toType(ExpressionTree.class, prepareQuery));

  /**
   * Matches on method invocation with Query as an argument
   */
  private static final MultiMatcher<MethodInvocationTree, ExpressionTree> queryArg =
      hasArguments(MatchType.AT_LEAST_ONE, queryExpr);
  private static final Matcher<MethodInvocationTree> queryArgButNotPrepare =
      allOf(queryArg, not(prepareQuery));
  private static final Matcher<Tree> hasMethodCallOtherThanPrepareThatTakesQueryType =
      contains(toType(MethodInvocationTree.class, queryArgButNotPrepare));

  /**
   * Matches on method invocation with Entity as an argument
   */
  private static final MultiMatcher<MethodInvocationTree, ExpressionTree> entityArg =
      hasArguments(MatchType.AT_LEAST_ONE, entityExpr);
  private static final Matcher<Tree> hasMethodCallThatTakesEntityType =
      contains(toType(MethodInvocationTree.class, entityArg));
  private static final Matcher<ExpressionTree> onlyNeedKey =
      anyOf(
          instanceMethod().onExactClass(ENTITY).named("clone"),
          instanceMethod().onExactClass(ENTITY).named("equals"),
          instanceMethod().onExactClass(ENTITY).named("getAppId"),
          instanceMethod().onExactClass(ENTITY).named("getAppIdNamespace"),
          instanceMethod().onExactClass(ENTITY).named("getKey"),
          instanceMethod().onExactClass(ENTITY).named("getKind"),
          instanceMethod().onExactClass(ENTITY).named("getNamespace"),
          instanceMethod().onExactClass(ENTITY).named("getParent"),
          instanceMethod().onExactClass(ENTITY).named("hashCode"),
          instanceMethod().onExactClass(ENTITY).named("toString"));

  /**
   * Matches on an Entity instance with anything but the listed in ONLY_NEED_KEY
   */
  public static final Matcher<Tree> hasEntityMethodsThatNeedMoreThanKeys =
      contains(toType(ExpressionTree.class, allOf(not(onlyNeedKey), anyInstanceMethodOnEntity)));

  private static final Matcher<ExpressionTree> hasEntityTypeArgument =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Type treeType = ASTHelpers.getType(tree);
          if (treeType == null) {
            return false;
          }

          List<Type> typeArgs = treeType.getTypeArguments();
          if (typeArgs == null || typeArgs.size() != 1) {
            return false;
          }

          return typeArgs.get(0).toString().equals(ENTITY);
        }
      };

  private static final Matcher<MethodInvocationTree> hasArgOfTypeIterableEntity =
      hasArguments(
          MatchType.AT_LEAST_ONE, allOf(isSubtypeOf("java.lang.Iterable"), hasEntityTypeArgument));

  private static final Matcher<Tree> hasMethodCallWithArgOfTypeIterableEntity =
      contains(toType(MethodInvocationTree.class, hasArgOfTypeIterableEntity));

  /**
   * Matches methods that fit the following requirements:
   * <ol>
   * <li>contains method call {@code DatastoreService#prepare}
   * <li>contains an instance method call on an Entity object
   * <li>no parameters of type Query, Entity, or Iterable
   * <li>does not contain a method call that takes an Entity or Iterable&lt;Entity&gt;
   * <li>does not contain a method call other than {@code DatastoreService#prepare} that takes a
   *     Query
   * </ol>
   */
  public static final Matcher<MethodTree> baseMatcher =
      allOf(
          hasPrepareMethodCallOnDatastoreService,
          hasInstanceMethodCallOnEntity,
          not(hasParamOfTypeQueryEntityOrIterable),
          not(hasMethodCallThatTakesEntityType),
          not(hasMethodCallWithArgOfTypeIterableEntity),
          not(hasMethodCallOtherThanPrepareThatTakesQueryType));
}
