/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableSet;

/**
 * Types that declare {@code equals}, but whose subtypes are not meaningfully comparable with each
 * other through it.
 *
 * <p>{@link TypeCompatibility} does not treat a common supertype in this set as a reason to
 * consider two types compatible for equality. {@link Object}, {@link Enum} and {@link Record} are
 * handled separately.
 */
public final class NonInheritedEqualsTypes {
  public static final ImmutableSet<String> CLASSES_WITH_NON_INHERITED_EQUALS =
      ImmutableSet.of(
          "com.google.common.base.Converter",
          "com.google.common.base.Equivalence",
          "com.google.common.base.Function",
          "com.google.common.base.Predicate",
          "com.google.common.base.Supplier",
          "com.google.common.flags.Flag",
          "com.google.common.flogger.MetadataKey",
          "com.google.common.reflect.AbstractInvocationHandler",
          "com.sun.tools.javac.code.Type",
          "java.awt.RenderingHints.Key",
          "java.lang.ProcessHandle",
          "java.net.InetAddress",
          "java.nio.file.Path",
          "java.security.BasicPermission",
          "java.security.Permission",
          "java.security.Principal",
          "java.text.AttributedCharacterIterator.Attribute",
          "java.text.Collator",
          "java.text.DateFormat",
          "java.text.NumberFormat",
          "java.time.Clock",
          "java.time.chrono.ChronoLocalDate",
          "java.time.chrono.ChronoLocalDateTime",
          "java.time.chrono.ChronoPeriod",
          "java.time.chrono.ChronoZonedDateTime",
          "java.util.Collection",
          "java.util.Comparator");

  private NonInheritedEqualsTypes() {}
}
