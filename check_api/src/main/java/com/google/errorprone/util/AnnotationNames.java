/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.util;

/** Fully qualified names of common annotations used in ErrorProne checks. */
public final class AnnotationNames {

  // keep-sorted start

  public static final String AFTER_TEMPLATE_ANNOTATION =
      "com.google.errorprone.refaster.annotation.AfterTemplate";
  public static final String BEFORE_TEMPLATE_ANNOTATION =
      "com.google.errorprone.refaster.annotation.BeforeTemplate";
  public static final String BUG_PATTERN_ANNOTATION = "com.google.errorprone.BugPattern";
  public static final String CAN_IGNORE_RETURN_VALUE_ANNOTATION =
      "com.google.errorprone.annotations.CanIgnoreReturnValue";
  public static final String COMPATIBLE_WITH_ANNOTATION =
      "com.google.errorprone.annotations.CompatibleWith";
  public static final String DO_NOT_CALL_ANNOTATION = "com.google.errorprone.annotations.DoNotCall";
  public static final String FORMAT_METHOD_ANNOTATION =
      "com.google.errorprone.annotations.FormatMethod";
  public static final String FORMAT_STRING_ANNOTATION =
      "com.google.errorprone.annotations.FormatString";
  public static final String IMMUTABLE_ANNOTATION = "com.google.errorprone.annotations.Immutable";
  public static final String LAZY_INIT_ANNOTATION =
      "com.google.errorprone.annotations.concurrent.LazyInit";
  public static final String MUST_BE_CLOSED_ANNOTATION =
      "com.google.errorprone.annotations.MustBeClosed";
  public static final String REPEATED_ANNOTATION =
      "com.google.errorprone.refaster.annotation.Repeated";
  public static final String RESTRICTED_API_ANNOTATION =
      "com.google.errorprone.annotations.RestrictedApi";
  public static final String THREAD_SAFE_ANNOTATION =
      "com.google.errorprone.annotations.ThreadSafe";
  public static final String VAR_ANNOTATION = "com.google.errorprone.annotations.Var";

  // keep-sorted end

  private AnnotationNames() {}
}
