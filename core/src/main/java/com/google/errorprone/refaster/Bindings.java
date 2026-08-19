/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * A type-safe binding environment mapping {@code Bindings.Key<V>} keys to values of type {@code V}.
 *
 * <p>Bindings are represented as a persistent linked chain. Adding a binding creates a new {@link
 * Node} pointing to the current head as its parent, and lookups traverse parent references towards
 * the root, naturally shadowing earlier bindings for the same key. Forking a {@code Bindings}
 * instance shares the underlying node chain with zero entry copying.
 *
 * @author Louis Wasserman
 */
public class Bindings {
  /**
   * A key type for a {@code Binding}. Users must subclass {@code Key} with a specific literal
   * {@code V} type.
   */
  public abstract static class Key<V> {
    private final String identifier;

    protected Key(String identifier) {
      this.identifier = checkNotNull(identifier);
    }

    public String getIdentifier() {
      return identifier;
    }

    TypeToken<V> getValueType() {
      return new TypeToken<V>(getClass()) {};
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getClass(), identifier);
    }

    @Override
    @SuppressWarnings("EqualsGetClass")
    public boolean equals(@Nullable Object obj) {
      // explicitly call getClass so that objects of different subclasses return false.
      if (obj != null && this.getClass() == obj.getClass()) {
        Key<?> key = (Key<?>) obj;
        return identifier.equals(key.identifier);
      }
      return false;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("identifier", identifier).toString();
    }
  }

  private static final class Node {
    final Key<?> key;
    final Object value;
    final @Nullable Node parent;

    Node(Key<?> key, Object value, @Nullable Node parent) {
      this.key = key;
      this.value = value;
      this.parent = parent;
    }
  }

  private @Nullable Node head;

  public static Bindings create() {
    return new Bindings(null);
  }

  public static Bindings create(Bindings bindings) {
    return new Bindings(bindings.head);
  }

  private Bindings(@Nullable Node head) {
    this.head = head;
  }

  public boolean isEmpty() {
    return head == null;
  }

  public boolean containsKey(Key<?> key) {
    checkNotNull(key);
    for (Node curr = head; curr != null; curr = curr.parent) {
      if (key.equals(curr.key)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked") // safe by runtime check in putBinding
  public <V> @Nullable V getBinding(Key<V> key) {
    checkNotNull(key);
    for (Node curr = head; curr != null; curr = curr.parent) {
      if (key.equals(curr.key)) {
        return (V) curr.value;
      }
    }
    return null;
  }

  @CanIgnoreReturnValue
  public <V> V putBinding(Key<V> key, V value) {
    checkNotNull(key, "key");
    checkNotNull(value, "value");
    Object castValue = key.getValueType().getRawType().cast(value);
    head = new Node(key, castValue, head);
    return value;
  }

  public void clearBinding(Key<?> key) {
    checkNotNull(key);
    List<Node> surviving = new ArrayList<>();
    Set<Key<?>> seenKeys = new HashSet<>();
    for (Node curr = head; curr != null; curr = curr.parent) {
      if (!curr.key.equals(key) && seenKeys.add(curr.key)) {
        surviving.add(curr);
      }
    }
    Node newHead = null;
    for (Node node : Lists.reverse(surviving)) {
      newHead = new Node(node.key, node.value, newHead);
    }
    this.head = newHead;
  }

  public void forEach(BiConsumer<? super Key<?>, Object> action) {
    checkNotNull(action);
    Set<Key<?>> seenKeys = new HashSet<>();
    for (Node curr = head; curr != null; curr = curr.parent) {
      if (seenKeys.add(curr.key)) {
        action.accept(curr.key, curr.value);
      }
    }
  }

  public boolean hasBindingForLocalVar(@Nullable Symbol symbol) {
    if (symbol == null) {
      return false;
    }
    for (Node curr = head; curr != null; curr = curr.parent) {
      if (curr.value instanceof LocalVarBinding binding && symbol.equals(binding.symbol())) {
        return true;
      }
    }
    return false;
  }

  public boolean hasFreeIdentMatching(Predicate<JCExpression> predicate) {
    checkNotNull(predicate);
    for (Node curr = head; curr != null; curr = curr.parent) {
      if (curr.key instanceof UFreeIdent.Key && curr.value instanceof JCExpression expr) {
        if (predicate.test(expr)) {
          return true;
        }
      }
    }
    return false;
  }

  public ImmutableMap<Key<?>, Object> asMap() {
    ImmutableMap.Builder<Key<?>, Object> builder = ImmutableMap.builder();
    forEach(builder::put);
    return builder.buildOrThrow();
  }

  @Override
  public String toString() {
    return asMap().toString();
  }
}
