/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.refaster.testdata;

import com.google.common.collect.Maps;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

/** Test data for {@code ShadowedNestedImportTemplate}. */
public class ShadowedNestedImportTemplateExample {
  Map.Entry<String, Integer> alreadyImported() {
    return new SimpleImmutableEntry<>("a", 1);
  }

  Map.Entry<String, Integer> notShadowed() {
    return new SimpleImmutableEntry<>("a", 1);
  }

  static class Base {
    static class SimpleImmutableEntry<K, V>
        extends java.util.AbstractMap.SimpleImmutableEntry<K, V> {
      SimpleImmutableEntry(K key, V value) {
        super(key, value);
      }
    }
  }

  static class InheritedMemberType extends Base {
    Map.Entry<String, Integer> example() {
      return new AbstractMap.SimpleImmutableEntry<>("a", 1);
    }
  }

  static class TopLevelClassShadowed {
    static class AbstractMap {}

    Map.Entry<String, Integer> example() {
      return new SimpleImmutableEntry<>("a", 1);
    }
  }

  Map<String, Integer> unrelated() {
    return Maps.newHashMap();
  }
}
