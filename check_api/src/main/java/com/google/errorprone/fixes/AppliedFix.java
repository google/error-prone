/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.fixes;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Represents the corrected source which we think was intended, by applying a Fix. This is used to
 * generate the "Did you mean?" snippet in the error message.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public record AppliedFix(String snippet, boolean isRemoveLine) {
  /**
   * Applies the suggestedFix to the source. Returns null if applying the fix results in no change
   * to the source, or a change only to imports.
   */
  public static @Nullable AppliedFix apply(
      CharSequence source, ErrorProneEndPosTable endPositions, Fix suggestedFix) {
    // We apply the replacements in ascending order here. Descending is simpler, since applying a
    // replacement can't change the index for future replacements, but it leads to quadratic
    // copying behavior as we constantly shift the tail of the file around in our StringBuilder.
    ImmutableSet<Replacement> replacements = ascending(suggestedFix.getReplacements(endPositions));
    if (replacements.isEmpty()) {
      return null;
    }

    String snippet = snippet(source, replacements);
    if (snippet.isEmpty()) {
      return new AppliedFix("to remove this line", /* isRemoveLine= */ true);
    }
    return new AppliedFix(snippet, /* isRemoveLine= */ false);
  }

  /**
   * The maximum distance we'll look for a newline before or after the snippet. If we don't find one
   * the snippet will just start or end in the middle of a line.
   */
  public static final int MAX_LINE_LENGTH = 100;

  private static String snippet(
      CharSequence sourceSequence, ImmutableSet<Replacement> replacements) {
    Replacement firstEdit = replacements.iterator().next();
    // Find a subrange of the source that should contain the entire first line that fixes are
    // applied to, and then only edit source and apply fixes in that range. This is a performance
    // optimization to avoid applying all of the fixes in very large files just to produce a
    // snippet.
    int startOffset = Math.max(0, firstEdit.startPosition() - MAX_LINE_LENGTH);
    int endOffset = Math.min(firstEdit.endPosition() + MAX_LINE_LENGTH, sourceSequence.length());
    Range<Integer> trimmed = Range.closedOpen(startOffset, endOffset);
    List<Replacement> shiftedReplacements = new ArrayList<>();
    for (Replacement replacement : replacements) {
      if (!replacement.range().isConnected(trimmed)) {
        continue;
      }
      if (replacement.endPosition() > endOffset) {
        endOffset = replacement.endPosition();
      }
      shiftedReplacements.add(
          Replacement.create(
              replacement.startPosition() - startOffset,
              replacement.endPosition() - startOffset,
              replacement.replaceWith()));
    }
    String replaced =
        applyReplacements(sourceSequence.subSequence(startOffset, endOffset), shiftedReplacements);
    // Find the changed line containing the first edit
    return firstEditedLine(replaced, shiftedReplacements.getFirst());
  }

  public static String applyReplacements(
      CharSequence source, ErrorProneEndPosTable endPositions, Fix fix) {
    return applyReplacements(source, fix.getReplacements(endPositions));
  }

  private static String applyReplacements(
      CharSequence source, Collection<Replacement> replacements) {
    StringBuilder replaced = new StringBuilder();
    int positionInOriginal = 0;
    for (Replacement repl : replacements) {
      checkArgument(
          repl.endPosition() <= source.length(),
          "End [%s] should not exceed source length [%s]",
          repl.endPosition(),
          source.length());

      // Write the unmodified content leading up to this change
      replaced.append(source, positionInOriginal, repl.startPosition());
      // And the modified content for this change
      replaced.append(repl.replaceWith());
      // Then skip everything from source between start and end
      positionInOriginal = repl.endPosition();
    }
    // Flush out any remaining content after the final change
    replaced.append(source, positionInOriginal, source.length());
    return replaced.toString();
  }

  /** Get the replacements in an appropriate order to apply correctly. */
  private static ImmutableSet<Replacement> ascending(Set<Replacement> set) {
    Replacements replacements = new Replacements();
    set.forEach(replacements::add);
    return replacements.ascending();
  }

  /**
   * Finds the full text of the first line that's changed. In this case "line" means "bracketed by
   * \n characters". We don't handle \r\n specially, because the strings that javac provides to
   * Error Prone have already been transformed from platform line endings to newlines (and even if
   * it didn't, the dangling \r characters would be handled by a trim() call).
   */
  private static String firstEditedLine(String content, Replacement firstEdit) {
    // We subtract 1 here because we want to find the first newline *before* the edit, not one
    // at its beginning.
    int startOfFirstEditedLine = content.lastIndexOf("\n", firstEdit.startPosition() - 1);
    int endOfFirstEditedLine = content.indexOf("\n", firstEdit.startPosition());
    if (startOfFirstEditedLine == -1) {
      startOfFirstEditedLine = 0; // Change to start of file with no preceding newline
    }
    if (endOfFirstEditedLine == -1) {
      // Change to last line of file
      endOfFirstEditedLine = content.length();
    }
    String snippet = content.substring(startOfFirstEditedLine, endOfFirstEditedLine);
    snippet = snippet.trim();
    if (snippet.contains("//")) {
      snippet = snippet.substring(0, snippet.indexOf("//")).trim();
    }
    return snippet;
  }
}
