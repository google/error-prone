/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * Specifies information about a type which may be a container specified by generic type arguments,
 * e.g. {@link com.google.errorprone.annotations.Immutable}.
 *
 * <p>Useful for providing information for immutable classes we can't easily annotate, e.g. those in
 * the JDK.
 */
@AutoValue
public abstract class AnnotationInfo {
  public abstract String typeName();

  public Set<String> containerOf() {
    return internalContainerOf();
  }

  abstract ImmutableSet<String> internalContainerOf();

  public static AnnotationInfo create(String typeName, Iterable<String> containerOf) {
    return new AutoValue_AnnotationInfo(typeName, ImmutableSet.copyOf(containerOf));
  }

  public static AnnotationInfo create(String typeName) {
    return create(typeName, ImmutableSet.<String>of());
  }
}
