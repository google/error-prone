/*
 * Copyright 2013 Google Inc. All rights reserved.
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
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A type-safe map from objects of type {@code Bindings.Key<V>}, which consist of a {@code String}
 * key and a {@code Bindings.Key} subclass, to values of type {@code V}.
 *
 * @author Louis Wasserman
 */
public class Bindings extends ForwardingMap<Bindings.Key<?>, Object> {
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
    public boolean equals(@Nullable Object obj) {
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

  private final Map<Key<?>, Object> contents;

  public static Bindings create() {
    return new Bindings();
  }

  public static <V> Bindings create(Key<V> key, V value) {
    Bindings result = create();
    result.putBinding(key, value);
    return result;
  }

  public static <V1, V2> Bindings create(Key<V1> key1, V1 value1, Key<V2> key2, V2 value2) {
    Bindings result = create();
    result.putBinding(key1, value1);
    result.putBinding(key2, value2);
    return result;
  }

  public static Bindings create(Bindings bindings) {
    return new Bindings(bindings);
  }

  private Bindings() {
    this(new HashMap<>());
  }

  Bindings(Bindings bindings) {
    this(Maps.newHashMap(bindings.contents));
  }

  private Bindings(Map<Key<?>, Object> contents) {
    this.contents = contents;
  }

  @Override
  protected Map<Key<?>, Object> delegate() {
    return contents;
  }

  @SuppressWarnings("unchecked")
  public <V> V getBinding(Key<V> key) {
    checkNotNull(key);
    return (V) super.get(key);
  }

  @SuppressWarnings("unchecked")
  public <V> V putBinding(Key<V> key, V value) {
    checkNotNull(value);
    return (V) super.put(key, value);
  }

  @Override
  public Object put(Key<?> key, Object value) {
    checkNotNull(key, "key");
    checkNotNull(value, "value");
    return super.put(key, key.getValueType().getRawType().cast(value));
  }

  @Override
  public void putAll(Map<? extends Key<?>, ? extends Object> map) {
    standardPutAll(map);
  }

  public Bindings unmodifiable() {
    return new Bindings(Collections.unmodifiableMap(contents));
  }
}
