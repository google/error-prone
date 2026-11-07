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

import com.google.errorprone.ClassName;

/** Fully qualified names of common annotations used in ErrorProne checks. */
public final class AnnotationNames {

  // keep-sorted start

  public static final ClassName AFTER_TEMPLATE_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.AfterTemplate");
  public static final ClassName AUTO_BUILDER_ANNOTATION =
      ClassName.of("com.google.auto.value.AutoBuilder");
  public static final ClassName AUTO_FACTORY_ANNOTATION =
      ClassName.of("com.google.auto.factory.AutoFactory");
  public static final ClassName AUTO_ONE_OF_ANNOTATION =
      ClassName.of("com.google.auto.value.AutoOneOf");
  public static final ClassName AUTO_VALUE_ANNOTATION =
      ClassName.of("com.google.auto.value.AutoValue");
  public static final ClassName AUTO_VALUE_BUILDER_ANNOTATION =
      ClassName.of("com.google.auto.value.AutoValue.Builder");
  public static final ClassName BEFORE_TEMPLATE_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.BeforeTemplate");
  public static final ClassName BUG_PATTERN_ANNOTATION =
      ClassName.of("com.google.errorprone.BugPattern");
  public static final ClassName CAN_IGNORE_RETURN_VALUE_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.CanIgnoreReturnValue");
  public static final ClassName CHECK_RETURN_VALUE_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.CheckReturnValue");
  public static final ClassName COMPATIBLE_WITH_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.CompatibleWith");
  public static final ClassName DEPRECATED_ANNOTATION = ClassName.of("java.lang.Deprecated");
  public static final ClassName DO_NOT_CALL_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.DoNotCall");
  public static final ClassName FORMAT_METHOD_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.FormatMethod");
  public static final ClassName FORMAT_STRING_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.FormatString");
  public static final ClassName IMMUTABLE_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.Immutable");
  public static final ClassName INCOMPATIBLE_MODIFIERS_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.IncompatibleModifiers");
  public static final ClassName INLINE_ME_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.InlineMe");
  public static final ClassName INLINE_ME_VALIDATION_DISABLED_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.InlineMeValidationDisabled");
  public static final ClassName LAZY_INIT_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.concurrent.LazyInit");
  public static final ClassName LENIENT_FORMAT_STRING_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.LenientFormatString");
  public static final ClassName MATCHES_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.Matches");
  public static final ClassName MUST_BE_CLOSED_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.MustBeClosed");
  public static final ClassName NOT_MATCHES_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.NotMatches");
  public static final ClassName NULL_MARKED_ANNOTATION =
      ClassName.of("org.jspecify.annotations.NullMarked");
  public static final ClassName NULL_UNMARKED_ANNOTATION =
      ClassName.of("org.jspecify.annotations.NullUnmarked");
  public static final ClassName OF_KIND_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.OfKind");
  public static final ClassName OVERRIDE_ANNOTATION = ClassName.of("java.lang.Override");
  public static final ClassName PLACEHOLDER_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.Placeholder");
  public static final ClassName REPEATABLE_ANNOTATION =
      ClassName.of("java.lang.annotation.Repeatable");
  public static final ClassName REPEATED_ANNOTATION =
      ClassName.of("com.google.errorprone.refaster.annotation.Repeated");
  public static final ClassName REQUIRED_MODIFIERS_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.RequiredModifiers");
  public static final ClassName RESTRICTED_API_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.RestrictedApi");
  public static final ClassName RETENTION_ANNOTATION =
      ClassName.of("java.lang.annotation.Retention");
  public static final ClassName SUPPRESS_WARNINGS_ANNOTATION =
      ClassName.of("java.lang.SuppressWarnings");
  public static final ClassName TARGET_ANNOTATION = ClassName.of("java.lang.annotation.Target");
  public static final ClassName THREAD_SAFE_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.ThreadSafe");
  public static final ClassName VAR_ANNOTATION =
      ClassName.of("com.google.errorprone.annotations.Var");

  // keep-sorted end

  private AnnotationNames() {}
}
