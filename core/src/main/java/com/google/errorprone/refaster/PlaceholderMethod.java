/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.refaster.UPlaceholderExpression.PlaceholderParamIdent;
import com.google.errorprone.refaster.annotation.Matches;
import com.google.errorprone.refaster.annotation.MayOptionallyUse;
import com.google.errorprone.refaster.annotation.NotMatches;
import com.google.errorprone.refaster.annotation.OfKind;
import com.google.errorprone.refaster.annotation.Placeholder;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

/**
 * Representation of a {@code Refaster} placeholder method, which can represent an arbitrary
 * operation on a specific set of expressions.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class PlaceholderMethod implements Serializable {
  static PlaceholderMethod create(
      CharSequence name,
      UType returnType,
      ImmutableMap<UVariableDecl, ImmutableClassToInstanceMap<Annotation>> parameters,
      ClassToInstanceMap<Annotation> annotations) {
    final boolean allowsIdentity = annotations.getInstance(Placeholder.class).allowsIdentity();
    final Class<? extends Matcher<? super ExpressionTree>> matchesClass =
        annotations.containsKey(Matches.class)
            ? UTemplater.getValue(annotations.getInstance(Matches.class))
            : null;
    final Class<? extends Matcher<? super ExpressionTree>> notMatchesClass =
        annotations.containsKey(NotMatches.class)
            ? UTemplater.getValue(annotations.getInstance(NotMatches.class))
            : null;
    final Predicate<Tree.Kind> allowedKinds =
        annotations.containsKey(OfKind.class)
            ? Predicates.<Tree.Kind>in(Arrays.asList(annotations.getInstance(OfKind.class).value()))
            : Predicates.<Tree.Kind>alwaysTrue();
    class PlaceholderMatcher implements Serializable, Matcher<ExpressionTree> {

      @Override
      public boolean matches(ExpressionTree t, VisitorState state) {
        try {
          return (allowsIdentity || !(t instanceof PlaceholderParamIdent))
              && (matchesClass == null || matchesClass.newInstance().matches(t, state))
              && (notMatchesClass == null || !notMatchesClass.newInstance().matches(t, state))
              && allowedKinds.apply(t.getKind());
        } catch (InstantiationException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return new AutoValue_PlaceholderMethod(
        StringName.of(name),
        returnType,
        parameters,
        new PlaceholderMatcher(),
        ImmutableClassToInstanceMap.<Annotation, Annotation>copyOf(annotations));
  }

  abstract StringName name();

  abstract UType returnType();

  abstract ImmutableMap<UVariableDecl, ImmutableClassToInstanceMap<Annotation>>
      annotatedParameters();

  abstract Matcher<ExpressionTree> matcher();

  abstract ImmutableClassToInstanceMap<Annotation> annotations();

  ImmutableSet<UVariableDecl> parameters() {
    return annotatedParameters().keySet();
  }

  /** Parameters which must be referenced in any tree matched to this placeholder. */
  Set<UVariableDecl> requiredParameters() {
    return Maps.filterValues(
            annotatedParameters(),
            (ImmutableClassToInstanceMap<Annotation> annotations) ->
                !annotations.containsKey(MayOptionallyUse.class))
        .keySet();
  }

  PlaceholderExpressionKey exprKey() {
    return new PlaceholderExpressionKey(name().contents(), this);
  }

  PlaceholderBlockKey blockKey() {
    return new PlaceholderBlockKey(name().contents(), this);
  }

  static final class PlaceholderExpressionKey extends Bindings.Key<JCExpression>
      implements Comparable<PlaceholderExpressionKey> {
    final PlaceholderMethod method;

    private PlaceholderExpressionKey(String str, PlaceholderMethod method) {
      super(str);
      this.method = method;
    }

    @Override
    public int compareTo(PlaceholderExpressionKey o) {
      return getIdentifier().compareTo(o.getIdentifier());
    }
  }

  static final class PlaceholderBlockKey extends Bindings.Key<List<JCStatement>> {
    final PlaceholderMethod method;

    private PlaceholderBlockKey(String str, PlaceholderMethod method) {
      super(str);
      this.method = method;
    }
  }
}
