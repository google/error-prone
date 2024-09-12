/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Information about known types and whether they're known to be safe or unsafe. */
public interface ThreadSafetyKnownTypes {
  /** Types that are known to be safe even if they're not annotated with an expected annotation. */
  ImmutableMap<String, AnnotationInfo> getKnownSafeClasses();

  /** Types that are known to be unsafe and don't need testing. */
  ImmutableSet<String> getKnownUnsafeClasses();

  /** Helper for building maps of classes to {@link AnnotationInfo}. */
  final class MapBuilder {
    final ImmutableMap.Builder<String, AnnotationInfo> mapBuilder = ImmutableMap.builder();

    @CanIgnoreReturnValue
    public MapBuilder addClasses(Set<Class<?>> clazzs) {
      clazzs.forEach(this::add);
      return this;
    }

    @CanIgnoreReturnValue
    public MapBuilder addStrings(List<String> classNames) {
      classNames.forEach(this::add);
      return this;
    }

    @CanIgnoreReturnValue
    public MapBuilder addAll(ImmutableMap<String, AnnotationInfo> map) {
      mapBuilder.putAll(map);
      return this;
    }

    @CanIgnoreReturnValue
    public MapBuilder add(Class<?> clazz, String... containerOf) {
      ImmutableSet<String> containerTyParams = ImmutableSet.copyOf(containerOf);
      HashSet<String> actualTyParams = new HashSet<>();
      for (TypeVariable<?> x : clazz.getTypeParameters()) {
        actualTyParams.add(x.getName());
      }
      SetView<String> difference = Sets.difference(containerTyParams, actualTyParams);
      if (!difference.isEmpty()) {
        throw new AssertionError(
            String.format(
                "For %s, please update the type parameter(s) from %s to %s",
                clazz, difference, actualTyParams));
      }
      mapBuilder.put(
          clazz.getName(),
          AnnotationInfo.create(clazz.getName(), ImmutableList.copyOf(containerOf)));
      return this;
    }

    @CanIgnoreReturnValue
    public MapBuilder add(String className, String... containerOf) {
      mapBuilder.put(
          className, AnnotationInfo.create(className, ImmutableList.copyOf(containerOf)));
      return this;
    }

    public ImmutableMap<String, AnnotationInfo> build() {
      return mapBuilder.buildKeepingLast();
    }
  }
}
