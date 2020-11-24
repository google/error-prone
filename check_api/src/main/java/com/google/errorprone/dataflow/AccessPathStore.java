/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.intersection;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.checkerframework.shaded.dataflow.analysis.AbstractValue;
import org.checkerframework.shaded.dataflow.analysis.Store;
import org.checkerframework.shaded.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.shaded.dataflow.expression.Receiver;

/**
 * Immutable map from local variables or heap access paths to their {@link AbstractValue}
 *
 * <p>To derive a new instance, {@linkplain #toBuilder() create a builder} from an old instance. To
 * start from scratch, call {@link #empty()}.
 *
 * @author bennostein@google.com (Benno Stein)
 */
@AutoValue
public abstract class AccessPathStore<V extends AbstractValue<V>>
    implements Store<AccessPathStore<V>>, AccessPathValues<V> {

  public abstract ImmutableMap<AccessPath, V> heap();

  private static <V extends AbstractValue<V>> AccessPathStore<V> create(
      ImmutableMap<AccessPath, V> heap) {
    return new AutoValue_AccessPathStore<>(heap);
  }

  @SuppressWarnings({"unchecked", "rawtypes"}) // fully variant
  private static final AccessPathStore<?> EMPTY =
      AccessPathStore.<AbstractValue>create(ImmutableMap.of());

  @SuppressWarnings("unchecked") // fully variant
  public static <V extends AbstractValue<V>> AccessPathStore<V> empty() {
    return (AccessPathStore<V>) EMPTY;
  }

  @Nullable
  private V getInformation(AccessPath ap) {
    return heap().get(checkNotNull(ap));
  }

  public Builder<V> toBuilder() {
    return new Builder<>(this);
  }

  @Override
  public V valueOfAccessPath(AccessPath path, V defaultValue) {
    V result = getInformation(path);
    return result != null ? result : defaultValue;
  }

  @Override
  public AccessPathStore<V> copy() {
    // No need to copy because it's immutable.
    return this;
  }

  @Override
  public AccessPathStore<V> leastUpperBound(AccessPathStore<V> other) {
    ImmutableMap.Builder<AccessPath, V> resultHeap = ImmutableMap.builder();
    for (AccessPath aPath : intersection(heap().keySet(), other.heap().keySet())) {
      resultHeap.put(aPath, heap().get(aPath).leastUpperBound(other.heap().get(aPath)));
    }
    return AccessPathStore.create(resultHeap.build());
  }

  @Override
  public AccessPathStore<V> widenedUpperBound(AccessPathStore<V> vAccessPathStore) {
    // No support for widening yet.
    return leastUpperBound(vAccessPathStore);
  }

  @Override
  public boolean canAlias(Receiver a, Receiver b) {
    return true;
  }

  @Override
  public String visualize(CFGVisualizer<?, AccessPathStore<V>, ?> cfgVisualizer) {
    throw new UnsupportedOperationException("DOT output not supported");
  }
  /**
   * Builder for {@link AccessPathStore} instances. To obtain an instance, obtain a {@link
   * AccessPathStore} (such as {@link AccessPathStore#empty()}), and call {@link
   * AccessPathStore#toBuilder() toBuilder()} on it.
   */
  public static final class Builder<V extends AbstractValue<V>> {
    private final Map<AccessPath, V> heap;

    Builder(AccessPathStore<V> prototype) {
      this.heap = new LinkedHashMap<>(prototype.heap());
    }

    public Builder<V> setInformation(AccessPath aPath, V value) {
      heap.put(checkNotNull(aPath), checkNotNull(value));
      return this;
    }

    public AccessPathStore<V> build() {
      return AccessPathStore.create(ImmutableMap.copyOf(heap));
    }
  }
}
