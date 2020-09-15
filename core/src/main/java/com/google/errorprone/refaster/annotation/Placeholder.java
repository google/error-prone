/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.refaster.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate a placeholder method.
 *
 * <p>A placeholder method is an abstract method in a Refaster template class which can represent an
 * arbitrary expression (if the return type is nonvoid), or zero or more statements (if the return
 * type is void), in terms of its arguments. For example,
 *
 * <pre>{@code
 * abstract class ComputeIfAbsent<K, V> {
 *  @Placeholder abstract V computeValue(K key);
 *
 *  @BeforeTemplate void getOrCompute(Map<K, V> map, K key) {
 *     V value = map.get(key);
 *     if (value == null) {
 *       map.put(key, value = computeValue(key));
 *     }
 *   }
 *
 *  @AfterTemplate void computeIfAbsent(Map<K, V> map, K key) {
 *     V value = map.computeIfAbsent(key, (K k) -> computeValue(k));
 *   }
 * }
 * }</pre>
 *
 * <p>Here, {@code computeValue} represents an arbitrary expression in terms of {@code key}, and the
 * {@code @AfterTemplate} rewrites that same expression in terms of the parameter of a lambda
 * expression.
 *
 * <p>For a multi-line example, consider
 *
 * <pre>{@code
 * abstract class TryWithResources<T extends AutoCloseable> {
 *  @Placeholder abstract T open();
 *  @Placeholder void process(T resource);
 *
 *  @BeforeTemplate void tryFinallyClose() {
 *     T resource = open();
 *     try {
 *       process(resource);
 *     } finally {
 *       resource.close();
 *     }
 *   }
 *
 *  @AfterTemplate void tryWithResource() {
 *     try (T resource = open()) {
 *       process(resource);
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Here, {@code process} is any block, though it must refer to {@code resource} in some way; it
 * is not permitted to reassign the contents of {@code resource}.
 *
 * <p>Placeholder methods are not permitted to refer to any local variables or parameters of the
 * {@code @BeforeTemplate} that are not passed to them as arguments. Additionally, they
 * <em>must</em> contain references to all arguments that <em>are</em> passed to them -- except
 * those corresponding to parameters annotated with {@link MayOptionallyUse}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Placeholder {
  // TODO(lowasser): consider putting forbiddenKinds here as an annotation parameter

  /**
   * Identifies whether the placeholder is allowed to match an expression which simply returns one
   * of the placeholder arguments unchanged.
   */
  boolean allowsIdentity() default false;
}
