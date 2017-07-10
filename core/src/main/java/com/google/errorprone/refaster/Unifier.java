/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.errorprone.SubContext;
import com.google.errorprone.refaster.Bindings.Key;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A mutable representation of an attempt to match a template source tree against a target source
 * tree.
 *
 * @author Louis Wasserman
 */
public final class Unifier {
  private final Bindings bindings;

  private final Context context;

  public Unifier(Context context) {
    this.bindings = Bindings.create();
    this.context = checkNotNull(context);
  }

  private Unifier(Context context, Bindings bindings) {
    this.context = new SubContext(context);
    this.bindings = Bindings.create(bindings);
  }

  /**
   * Returns a {@code Unifier} containing all the bindings from this {@code Unifier}, but which can
   * succeed or fail independently of this {@code Unifier}.
   */
  public Unifier fork() {
    return new Unifier(context, bindings);
  }

  public Types types() {
    return Types.instance(context);
  }

  public JCExpression thisExpression(Type type) {
    return TreeMaker.instance(context).This(type);
  }

  public Inliner createInliner() {
    return new Inliner(context, bindings);
  }

  @Nullable
  public <V> V getBinding(Key<V> key) {
    return bindings.getBinding(key);
  }

  public <V> V putBinding(Key<V> key, V value) {
    checkArgument(!bindings.containsKey(key), "Cannot bind %s more than once", key);
    return bindings.putBinding(key, value);
  }

  public <V> V replaceBinding(Key<V> key, V value) {
    checkArgument(bindings.containsKey(key), "Binding for %s does not exist", key);
    return bindings.putBinding(key, value);
  }

  public void clearBinding(Key<?> key) {
    bindings.remove(key);
  }

  public Bindings getBindings() {
    return bindings.unmodifiable();
  }

  public Context getContext() {
    return context;
  }

  @Override
  public String toString() {
    return "Unifier{" + bindings + "}";
  }

  public static <T, U extends Unifiable<? super T>> Function<Unifier, Choice<Unifier>> unifications(
      @Nullable final U unifiable, @Nullable final T target) {
    return (Unifier unifier) -> unifyNullable(unifier, unifiable, target);
  }

  public static <T, U extends Unifiable<? super T>> Choice<Unifier> unifyNullable(
      Unifier unifier, @Nullable final U unifiable, @Nullable final T target) {
    if (target == null && unifiable == null) {
      return Choice.of(unifier);
    } else if (target == null || unifiable == null) {
      return Choice.none();
    } else {
      return unifiable.unify(target, unifier);
    }
  }

  public static <T, U extends Unifiable<? super T>> Function<Unifier, Choice<Unifier>> unifications(
      @Nullable final List<U> toUnify, @Nullable final List<? extends T> targets) {
    return unifications(toUnify, targets, false);
  }

  public static <T, U extends Unifiable<? super T>> Function<Unifier, Choice<Unifier>> unifications(
      @Nullable final List<U> toUnify,
      @Nullable final List<? extends T> targets,
      final boolean allowVarargs) {
    return (Unifier unifier) -> unifyList(unifier, toUnify, targets, allowVarargs);
  }

  /**
   * Returns all successful unification paths from the specified {@code Unifier} unifying the
   * specified lists, disallowing varargs.
   */
  public static <T, U extends Unifiable<? super T>> Choice<Unifier> unifyList(
      Unifier unifier, @Nullable List<U> toUnify, @Nullable final List<? extends T> targets) {
    return unifyList(unifier, toUnify, targets, false);
  }

  /**
   * Returns all successful unification paths from the specified {@code Unifier} unifying the
   * specified lists, allowing varargs if and only if {@code allowVarargs} is true.
   */
  public static <T, U extends Unifiable<? super T>> Choice<Unifier> unifyList(
      Unifier unifier,
      @Nullable List<U> toUnify,
      @Nullable final List<? extends T> targets,
      boolean allowVarargs) {
    if (toUnify == null && targets == null) {
      return Choice.of(unifier);
    } else if (toUnify == null
        || targets == null
        || (allowVarargs
            ? toUnify.size() - 1 > targets.size()
            : toUnify.size() != targets.size())) {
      return Choice.none();
    }
    Choice<Unifier> choice = Choice.of(unifier);
    int index;
    for (index = 0; index < toUnify.size(); index++) {
      U toUnifyNext = toUnify.get(index);
      if (allowVarargs && toUnifyNext instanceof URepeated) {
        final URepeated repeated = (URepeated) toUnifyNext;
        final int startIndex = index;
        return choice
            .condition(index + 1 == toUnify.size())
            .thenOption(
                new Function<Unifier, Optional<Unifier>>() {
                  @Override
                  public Optional<Unifier> apply(Unifier unifier) {
                    List<JCExpression> expressions = new ArrayList<>();
                    for (int j = startIndex; j < targets.size(); j++) {
                      Optional<Unifier> forked =
                          repeated.unify((JCTree) targets.get(j), unifier.fork()).first();
                      if (!forked.isPresent()) {
                        return Optional.absent();
                      }
                      JCExpression boundExpr = repeated.getUnderlyingBinding(forked.get());
                      if (boundExpr == null) {
                        return Optional.absent();
                      }
                      expressions.add(boundExpr);
                    }
                    unifier.putBinding(repeated.key(), expressions);
                    return Optional.of(unifier);
                  }
                });
      }
      if (index >= targets.size()) {
        return Choice.none();
      }
      choice = choice.thenChoose(unifications(toUnifyNext, targets.get(index)));
    }
    if (index < targets.size()) {
      return Choice.none();
    }
    return choice;
  }
}
