/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.dataflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.intersection;
import static javax.lang.model.element.ElementKind.EXCEPTION_PARAMETER;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;
import static javax.lang.model.element.ElementKind.PARAMETER;
import static javax.lang.model.element.ElementKind.RESOURCE_VARIABLE;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.javacutil.trees.DetachedVarSymbol;

/**
 * Immutable map from each local variable to its {@link AbstractValue}. Note that, while the
 * interface is written in terms of <b>nodes</b>, the stored data is indexed by variable
 * <b>declaration</b>, so values persist across nodes.
 *
 * <p>To derive a new instance, {@linkplain #toBuilder() create a builder} from an old instance. To
 * start from scratch, call {@link #empty()}.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public final class LocalStore<V extends AbstractValue<V>>
    implements Store<LocalStore<V>>, LocalVariableValues<V> {

  @SuppressWarnings({"unchecked", "rawtypes"}) // fully variant
  private static final LocalStore<?> EMPTY = new LocalStore(ImmutableMap.of());

  @SuppressWarnings("unchecked") // fully variant
  public static <V extends AbstractValue<V>> LocalStore<V> empty() {
    return (LocalStore<V>) EMPTY;
  }

  private final ImmutableMap<Element, V> contents;

  private LocalStore(Map<Element, V> contents) {
    this.contents = ImmutableMap.copyOf(contents);
  }

  @Override
  public V valueOfLocalVariable(LocalVariableNode node, V defaultValue) {
    V result = getInformation(node.getElement());
    return result != null ? result : defaultValue;
  }

  /**
   * Returns the value for the given variable. {@code element} must come from a call to {@link
   * LocalVariableNode#getElement()} or {@link
   * org.checkerframework.javacutil.TreeUtils#elementFromDeclaration} ({@link
   * org.checkerframework.dataflow.cfg.node.VariableDeclarationNode#getTree()}).
   */
  @Nullable
  private V getInformation(Element element) {
    checkElementType(element);
    return contents.get(checkNotNull(element));
  }

  public Builder<V> toBuilder() {
    return new Builder<>(this);
  }

  /**
   * Builder for {@link LocalStore} instances. To obtain an instance, obtain a {@link LocalStore}
   * (such as {@link LocalStore#empty()}), and call {@link LocalStore#toBuilder() toBuilder()} on
   * it.
   */
  public static final class Builder<V extends AbstractValue<V>> {
    private final Map<Element, V> contents;

    Builder(LocalStore<V> prototype) {
      contents = new HashMap<>(prototype.contents);
    }

    /**
     * Sets the value for the given variable. {@code element} must come from a call to {@link
     * LocalVariableNode#getElement()} or {@link
     * org.checkerframework.javacutil.TreeUtils#elementFromDeclaration} ({@link
     * org.checkerframework.dataflow.cfg.node.VariableDeclarationNode#getTree()}).
     */
    public Builder<V> setInformation(Element element, V value) {
      checkElementType(element);
      contents.put(checkNotNull(element), checkNotNull(value));
      return this;
    }

    public LocalStore<V> build() {
      return new LocalStore<>(contents);
    }
  }

  @Override
  public LocalStore<V> copy() {
    // No need to copy because it's immutable.
    return this;
  }

  @Override
  public LocalStore<V> leastUpperBound(LocalStore<V> other) {
    Builder<V> result = LocalStore.<V>empty().toBuilder();
    for (Element var : intersection(contents.keySet(), other.contents.keySet())) {
      result.contents.put(var, contents.get(var).leastUpperBound(other.contents.get(var)));
    }
    return result.build();
  }

  @Override
  public LocalStore<V> widenedUpperBound(LocalStore<V> vLocalStore) {
    // No support for widening yet.
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LocalStore)) {
      return false;
    }
    LocalStore<?> other = (LocalStore<?>) o;
    return contents.equals(other.contents);
  }

  @Override
  public int hashCode() {
    return contents.hashCode();
  }

  @Override
  public String toString() {
    return contents.toString();
  }

  @Override
  public boolean canAlias(FlowExpressions.Receiver a, FlowExpressions.Receiver b) {
    return true;
  }

  @Override
  public void visualize(CFGVisualizer<?, LocalStore<V>, ?> cfgVisualizer) {
    throw new UnsupportedOperationException("DOT output not supported");
  }

  private static void checkElementType(Element element) {
    checkArgument(
        element.getKind() == LOCAL_VARIABLE
            || element.getKind() == PARAMETER
            || element.getKind() == EXCEPTION_PARAMETER
            || element.getKind() == RESOURCE_VARIABLE
            // The following is a workaround for b/80179088. DetachedVarSymbol is used for temp
            // variables introduced into dataflow CFGs. These are always local variables, but can
            // sometimes be reported as fields.
            || element instanceof DetachedVarSymbol,
        "unexpected element type: %s (%s)",
        element.getKind(),
        element);
  }
}
