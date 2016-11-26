/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;

/** Integration test testing a hypothetical multimap interface */
public class IncompatibleArgumentTypeMultimapIntegration {
  interface Multimap<K, V> {
    boolean containsKey(@CompatibleWith("K") Object key);

    boolean containsValue(@CompatibleWith("V") Object value);

    boolean containsEntry(@CompatibleWith("K") Object key, @CompatibleWith("V") Object value);

    boolean containsAllKeys(@CompatibleWith("K") Object key, Object... others);
  }

  class MyMultimap<K, V> implements Multimap<K, V> {
    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public boolean containsValue(Object value) {
      return false;
    }

    @Override
    public boolean containsEntry(Object key, Object value) {
      return false;
    }

    @Override
    public boolean containsAllKeys(Object key, Object... keys) {
      return false;
    }
  }

  void testRegularValid(Multimap<Integer, String> intToString) {
    intToString.containsKey(123);
    intToString.containsEntry(123, "abc");
    intToString.containsValue("def");
    // 0-entry vararg doesn't crash
    intToString.containsAllKeys(123);
  }

  static <K extends Number, V extends String> void testIncompatibleWildcards(
      Multimap<? extends K, ? extends V> map, K key, V value) {
    map.containsKey(key);
    map.containsValue(value);
    map.containsEntry(key, value);

    // BUG: Diagnostic contains: V is not compatible with the required type: K
    map.containsEntry(value, key);
    // BUG: Diagnostic contains: K is not compatible with the required type: V
    map.containsValue(key);
    // BUG: Diagnostic contains: V is not compatible with the required type: K
    map.containsKey(value);
  }

  void testVarArgs(Multimap<Integer, String> intToString) {
    // Validates the first, not the varags params
    intToString.containsAllKeys(123, 123, 123);
    // TODO(glorioso): If we make it work with varargs, this should fail
    intToString.containsAllKeys(123, 123, "a");

    Integer[] keys = {123, 345};
    intToString.containsAllKeys(123, (Object[]) keys);
  }
}
