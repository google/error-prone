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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.bugpatterns.javadoc.JavadocTag.blockTag;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.annotations.Immutable;
import javax.inject.Inject;

/** Other commonly-used javadoc tags which we allow. */
@Immutable
public final class KnownTags {

  /** Non-standard commonly-used tags which we should allow. */
  private static final ImmutableSet<JavadocTag> KNOWN_OTHER_TAGS =
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

  private final ImmutableSet<JavadocTag> knownTags;

  @Inject
  KnownTags(ErrorProneFlags flags) {
    this.knownTags =
        Streams.concat(
                KNOWN_OTHER_TAGS.stream(),
                flags.getListOrEmpty("Javadoc:customBlockTags").stream().map(JavadocTag::blockTag),
                flags.getListOrEmpty("Javadoc:customInlineTags").stream()
                    .map(JavadocTag::inlineTag))
            .collect(toImmutableSet());
  }

  public boolean isKnownTag(JavadocTag tag) {
    return knownTags.contains(tag);
  }
}
