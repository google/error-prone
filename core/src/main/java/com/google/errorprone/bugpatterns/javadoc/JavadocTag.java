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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/** Describes Javadoc tags, and contains lists of valid tags. */
@AutoValue
abstract class JavadocTag {

  /** Non-standard commonly-used tags which we should allow. */
  static final ImmutableSet<JavadocTag> KNOWN_OTHER_TAGS =
      ImmutableSet.of(
          blockTag("apiNote"),
          blockTag("attr"), // commonly used by Android
          blockTag("contact"),
          blockTag("hide"),
          blockTag("implNote"),
          blockTag("implSpec"),
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
