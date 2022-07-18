/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import java.util.stream.Stream;

/** Describes Javadoc tags, and contains lists of valid tags. */
@Immutable
@AutoValue
abstract class JavadocTag {

  /** Non-standard commonly-used tags which we should allow. */
  static final ImmutableSet<JavadocTag> KNOWN_OTHER_TAGS =
      ImmutableSet.of(
          blockTag("apiNote"),
          blockTag("attr"), // commonly used by Android
          blockTag("contact"),
          blockTag("fails"), // commonly used tag for denoting async failure modes
          blockTag("hide"),
          blockTag("implNote"),
          blockTag("implSpec"),
          blockTag("removed"), // Used in the android framework (metalava)
          blockTag("required"),
          blockTag("team"));

  private static final ImmutableSet<JavadocTag> COMMON_TAGS =
      ImmutableSet.of(
          inlineTag("code"),
          blockTag("deprecated"),
          inlineTag("docRoot"),
          inlineTag("link"),
          inlineTag("linkplain"),
          inlineTag("literal"),
          blockTag("see"),
          blockTag("since"));

  static final ImmutableSet<JavadocTag> VALID_CLASS_TAGS =
      ImmutableSet.<JavadocTag>builder()
          .addAll(COMMON_TAGS)
          .add(
              blockTag("author"),
              inlineTag("inheritDoc"),
              blockTag("param"),
              inlineTag("value"),
              blockTag("version"))
          .build();

  static final ImmutableSet<JavadocTag> VALID_VARIABLE_TAGS =
      ImmutableSet.<JavadocTag>builder()
          .addAll(COMMON_TAGS)
          .add(
              blockTag("serial"),
              blockTag("serialData"),
              blockTag("serialField"),
              inlineTag("value"))
          .build();

  static final ImmutableSet<JavadocTag> VALID_METHOD_TAGS =
      ImmutableSet.<JavadocTag>builder()
          .addAll(COMMON_TAGS)
          .add(
              blockTag("author"),
              blockTag("exception"),
              inlineTag("inheritDoc"),
              blockTag("param"),
              blockTag("return"),
              blockTag("serial"),
              blockTag("throws"),
              blockTag("serialData"),
              blockTag("serialField"),
              inlineTag("value"),
              blockTag("version"))
          .build();

  static final ImmutableSet<JavadocTag> ALL_INLINE_TAGS =
      Stream.of(VALID_CLASS_TAGS, VALID_VARIABLE_TAGS, VALID_METHOD_TAGS)
          .flatMap(ImmutableSet::stream)
          .filter(tag -> tag.type() == TagType.INLINE)
          .collect(toImmutableSet());

  abstract String name();

  abstract TagType type();

  enum TagType {
    BLOCK,
    INLINE
  }

  static JavadocTag blockTag(String name) {
    return of(name, TagType.BLOCK);
  }

  static JavadocTag inlineTag(String name) {
    return of(name, TagType.INLINE);
  }

  private static JavadocTag of(String name, TagType type) {
    return new AutoValue_JavadocTag(name, type);
  }
}
