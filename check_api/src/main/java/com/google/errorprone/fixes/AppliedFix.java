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
import com.google.common.collect.Iterables;
import com.sun.tools.javac.tree.EndPosTable;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Represents the corrected source which we think was intended, by applying a Fix. This is used to
 * generate the "Did you mean?" snippet in the error message.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class AppliedFix {
  private final String snippet;
  private final boolean isRemoveLine;

  private AppliedFix(String snippet, boolean isRemoveLine) {
    this.snippet = snippet;
    this.isRemoveLine = isRemoveLine;
  }

  public CharSequence getNewCodeSnippet() {
    return snippet;
  }

  public boolean isRemoveLine() {
    return isRemoveLine;
  }

  public static class Applier {
    private final CharSequence source;
    private final EndPosTable endPositions;

    public Applier(CharSequence source, EndPosTable endPositions) {
      this.source = source;
      this.endPositions = endPositions;
    }

    /**
     * Applies the suggestedFix to the source. Returns null if applying the fix results in no change
     * to the source, or a change only to imports.
     */
    @Nullable
    public AppliedFix apply(Fix suggestedFix) {
      // We apply the replacements in ascending order here. Descending is simpler, since applying a
      // replacement can't change the index for future replacements, but it leads to quadratic
      // copying behavior as we constantly shift the tail of the file around in our StringBuilder.
      ImmutableSet<Replacement> replacements =
          ascending(suggestedFix.getReplacements(endPositions));
      if (replacements.isEmpty()) {
        return null;
      }

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

      // Find the changed line containing the first edit
      String snippet = firstEditedLine(replaced, Iterables.get(replacements, 0));
      if (snippet.isEmpty()) {
        return new AppliedFix("to remove this line", /* isRemoveLine= */ true);
      }
      return new AppliedFix(snippet, /* isRemoveLine= */ false);
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
    private static String firstEditedLine(StringBuilder content, Replacement firstEdit) {
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

  public static Applier fromSource(CharSequence source, EndPosTable endPositions) {
    return new Applier(source, endPositions);
  }
}
