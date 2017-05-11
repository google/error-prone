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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.auto.value.AutoValue;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Extracts the necessary information from a {@link MethodInvocationTree} to check whether calls to
 * a method are using incompatible types and to emit a helpful error message.
 */
abstract class AbstractCollectionIncompatibleTypeMatcher {

  /**
   * Returns a matcher for the appropriate method invocation for this matcher. For example, this
   * might match {@link Collection#remove(Object)} or {@link Map#containsKey(Object)}.
   */
  abstract Matcher<ExpressionTree> methodMatcher();

  /**
   * Extracts the source type that must be castable to the target type. For example, in this code
   * sample:
   *
   * <pre>{@code
   * Collection<Integer> collection;
   * collection.contains("foo");
   * }</pre>
   *
   * The source type is String.
   *
   * @return the source type or null if not available
   */
  @Nullable
  abstract Type extractSourceType(MethodInvocationTree tree, VisitorState state);

  /**
   * Returns the AST node from which the source type was extracted. Needed to produce readable error
   * messages. For example, in this code sample:
   *
   * <pre>{@code
   * Collection<Integer> collection;
   * collection.contains("foo");
   * }</pre>
   *
   * The source tree is "foo".
   *
   * @return the source AST node or null if not available
   */
  @Nullable
  abstract ExpressionTree extractSourceTree(MethodInvocationTree tree, VisitorState state);

  /**
   * Extracts the target type to which the source type must be castable. For example, in this code
   * sample:
   *
   * <pre>{@code
   * Collection<Integer> collection;
   * collection.contains("foo");
   * }</pre>
   *
   * The target type is Integer.
   *
   * @return the target type or null if not available
   */
  @Nullable
  abstract Type extractTargetType(MethodInvocationTree tree, VisitorState state);

  @AutoValue
  abstract static class MatchResult {
    public abstract ExpressionTree sourceTree();

    public abstract Type sourceType();

    public abstract Type targetType();

    public abstract AbstractCollectionIncompatibleTypeMatcher matcher();

    public static MatchResult create(
        ExpressionTree sourceTree,
        Type sourceType,
        Type targetType,
        AbstractCollectionIncompatibleTypeMatcher matcher) {
      return new AutoValue_AbstractCollectionIncompatibleTypeMatcher_MatchResult(
          sourceTree, sourceType, targetType, matcher);
    }
  }

  @Nullable
  public final MatchResult matches(MethodInvocationTree tree, VisitorState state) {
    if (!methodMatcher().matches(tree, state)) {
      return null;
    }

    ExpressionTree sourceTree = extractSourceTree(tree, state);
    Type sourceType = extractSourceType(tree, state);
    Type targetType = extractTargetType(tree, state);

    if (sourceTree == null || sourceType == null || targetType == null) {
      return null;
    }

    return MatchResult.create(
        extractSourceTree(tree, state),
        extractSourceType(tree, state),
        extractTargetType(tree, state),
        this);
  }

  /**
   * Extracts the appropriate type argument from a specific supertype of the given {@code type}.
   * This handles the case when a subtype has different type arguments than the expected type. For
   * example, {@code ClassToInstanceMap<T>} implements {@code Map<Class<? extends T>, T>}.
   *
   * @param type the (sub)type from which to extract the type argument
   * @param superTypeSym the symbol of the supertype on which the type parameter is defined
   * @param typeArgIndex the index of the type argument to extract from the supertype
   * @param types the {@link Types} utility class from the {@link VisitorState}
   * @return the type argument, if defined, or null otherwise
   */
  @Nullable
  protected static final Type extractTypeArgAsMemberOfSupertype(
      Type type, Symbol superTypeSym, int typeArgIndex, Types types) {
    Type collectionType = types.asSuper(type, superTypeSym);
    if (collectionType == null) {
      return null;
    }
    com.sun.tools.javac.util.List<Type> tyargs = collectionType.getTypeArguments();
    if (tyargs.size() <= typeArgIndex) {
      // Collection is raw, nothing we can do.
      return null;
    }

    return tyargs.get(typeArgIndex);
  }
}
