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
  public static final String AUTO_BUILDER_ANNOTATION = "com.google.auto.value.AutoBuilder";
  public static final String AUTO_FACTORY_ANNOTATION = "com.google.auto.factory.AutoFactory";
  public static final String AUTO_VALUE_ANNOTATION = "com.google.auto.value.AutoValue";
  public static final String BEFORE_TEMPLATE_ANNOTATION =
      "com.google.errorprone.refaster.annotation.BeforeTemplate";
  public static final String BUG_PATTERN_ANNOTATION = "com.google.errorprone.BugPattern";
  public static final String CAN_IGNORE_RETURN_VALUE_ANNOTATION =
      "com.google.errorprone.annotations.CanIgnoreReturnValue";
  public static final String CHECK_RETURN_VALUE_ANNOTATION =
      "com.google.errorprone.annotations.CheckReturnValue";
  public static final String COMPATIBLE_WITH_ANNOTATION =
      "com.google.errorprone.annotations.CompatibleWith";
  public static final String DEPRECATED_ANNOTATION = "java.lang.Deprecated";
  public static final String DO_NOT_CALL_ANNOTATION = "com.google.errorprone.annotations.DoNotCall";
  public static final String FORMAT_METHOD_ANNOTATION =
      "com.google.errorprone.annotations.FormatMethod";
  public static final String FORMAT_STRING_ANNOTATION =
      "com.google.errorprone.annotations.FormatString";
  public static final String IMMUTABLE_ANNOTATION = "com.google.errorprone.annotations.Immutable";
  public static final String INCOMPATIBLE_MODIFIERS_ANNOTATION =
      "com.google.errorprone.annotations.IncompatibleModifiers";
  public static final String INLINE_ME_ANNOTATION = "com.google.errorprone.annotations.InlineMe";
  public static final String INLINE_ME_VALIDATION_DISABLED_ANNOTATION =
      "com.google.errorprone.annotations.InlineMeValidationDisabled";
  public static final String LAZY_INIT_ANNOTATION =
      "com.google.errorprone.annotations.concurrent.LazyInit";
  public static final String LENIENT_FORMAT_STRING_ANNOTATION =
      "com.google.errorprone.annotations.LenientFormatString";
  public static final String MATCHES_ANNOTATION =
      "com.google.errorprone.refaster.annotation.Matches";
  public static final String MUST_BE_CLOSED_ANNOTATION =
      "com.google.errorprone.annotations.MustBeClosed";
  public static final String NOT_MATCHES_ANNOTATION =
      "com.google.errorprone.refaster.annotation.NotMatches";
  public static final String NULL_MARKED_ANNOTATION = "org.jspecify.annotations.NullMarked";
  public static final String NULL_UNMARKED_ANNOTATION = "org.jspecify.annotations.NullUnmarked";
  public static final String OF_KIND_ANNOTATION =
      "com.google.errorprone.refaster.annotation.OfKind";
  public static final String OVERRIDE_ANNOTATION = "java.lang.Override";
  public static final String REPEATABLE_ANNOTATION = "java.lang.annotation.Repeatable";
  public static final String REPEATED_ANNOTATION =
      "com.google.errorprone.refaster.annotation.Repeated";
  public static final String REQUIRED_MODIFIERS_ANNOTATION =
      "com.google.errorprone.annotations.RequiredModifiers";
  public static final String RESTRICTED_API_ANNOTATION =
      "com.google.errorprone.annotations.RestrictedApi";
  public static final String RETENTION_ANNOTATION = "java.lang.annotation.Retention";
  public static final String SUPPRESS_WARNINGS_ANNOTATION = "java.lang.SuppressWarnings";
  public static final String TARGET_ANNOTATION = "java.lang.annotation.Target";
  public static final String THREAD_SAFE_ANNOTATION =
      "com.google.errorprone.annotations.ThreadSafe";
  public static final String VAR_ANNOTATION = "com.google.errorprone.annotations.Var";

  // keep-sorted end

  private AnnotationNames() {}
}
