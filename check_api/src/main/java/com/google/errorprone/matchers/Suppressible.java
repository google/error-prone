/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.BugPattern;
import java.lang.annotation.Annotation;
import java.util.Set;

/** @author alexeagle@google.com (Alex Eagle) */
public interface Suppressible {
  /**
   * Returns all of the name strings that this checker should respect as part of a
   * {@code @SuppressWarnings} annotation.
   */
  Set<String> allNames();

  /** The canonical name of the check. */
  String canonicalName();

  /**
   * Returns how this checker can be suppressed (e.g., via {@code @SuppressWarnings} or a custom
   * suppression annotation.
   */
  BugPattern.Suppressibility suppressibility();

  /** Returns the custom suppression annotations for this checker, if custom suppression is used. */
  Set<Class<? extends Annotation>> customSuppressionAnnotations();
}
