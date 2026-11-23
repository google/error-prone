/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone;

import com.google.common.collect.MapMaker;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;

/**
 * A value that belongs to one compilation.
 *
 * <p>Values are stored in a map owned by the compilation's {@code Context}, so they are never
 * shared between compilations and live exactly as long as the compilation, even if this object is
 * held in a static field. The last value set is also remembered through a weak reference, which
 * makes repeated lookups in the same compilation cheap without keeping it alive afterwards.
 */
final class PerCompilationValue<T> {

  /** The values of one compilation, owned by its {@code Context}. */
  static final class Values {
    private static final AtomicLong lastId = new AtomicLong();

    // Weak (identity) keys: a key that is created for each call instead of being kept in a field
    // must not stay in the map for the rest of the compilation.
    private final ConcurrentMap<Object, Object> map = new MapMaker().weakKeys().makeMap();

    /** Identifies the compilation, without referencing it. */
    final long id = lastId.incrementAndGet();
  }

  /**
   * A weak reference to the last value set, with the id of its compilation. Only an optimization:
   * repeated lookups in the same compilation, the common case, return it without a map lookup.
   */
  private static final class Last<T> extends WeakReference<T> {
    private final long compilationId;

    Last(T value, long compilationId) {
      super(value);
      this.compilationId = compilationId;
    }
  }

  private volatile @Nullable Last<T> last;

  /** Returns the value for the current compilation, or {@code null} if none was set. */
  @Nullable T get(VisitorState state) {
    // Kept small so that it is inlined: the common case is a repeated lookup in one compilation.
    Last<T> l = last;
    if (l != null && l.compilationId == state.compilationId()) {
      T value = l.get();
      if (value != null) {
        return value;
      }
    }
    return getFromMap(state);
  }

  @SuppressWarnings("unchecked") // only this object stores values under its key
  private @Nullable T getFromMap(VisitorState state) {
    return (T) state.compilationValues().map.get(this);
  }

  /** Sets the value for the current compilation. */
  void set(VisitorState state, T value) {
    Values values = state.compilationValues();
    values.map.put(this, value);
    last = new Last<>(value, values.id);
  }
}
