/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableRangeSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.CompilationUnitTree;

/** Replaces printable ASCII unicode escapes with the literal version. */
@BugPattern(
    name = "UnicodeEscape",
    summary =
        "Using unicode escape sequences for printable ASCII characters is obfuscated, and"
            + " potentially dangerous.",
    severity = WARNING)
public final class UnicodeEscape extends BugChecker implements CompilationUnitTreeMatcher {
  private final Supplier<ImmutableRangeSet<Integer>> suppressedRegions =
      VisitorState.memoize(this::suppressedRegions);

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new UnicodeScanner(state.getSourceCode().toString(), state).scan();
    return NO_MATCH;
  }

  private final class UnicodeScanner {
    private final String source;
    private final VisitorState state;

    private int position = 0;
    private char currentCharacter = 0;
    private boolean isUnicode = false;
    private int lastBackslash = 0;

    private UnicodeScanner(String source, VisitorState state) {
      this.source = source;
      this.state = state;
      this.currentCharacter = source.charAt(0);
    }

    public void scan() {
      for (; position < source.length(); processCharacter()) {
        if (isUnicode && isBanned(currentCharacter)) {
          if (currentCharacter == '\\' && peek() == 'u') {
            continue;
          }
          if (suppressedRegions.get(state).contains(position)) {
            continue;
          }
          state.reportMatch(
              describeMatch(
                  new FixedPosition(state.getPath().getCompilationUnit(), position),
                  SuggestedFix.replace(
                      lastBackslash, position + 1, Character.toString(currentCharacter))));
        }
      }
    }

    private void processCharacter() {
      if (currentCharacter == '\\') {
        lastBackslash = position;
        nextCharacter();
        // The isUnicode check is important because the Unicode escape for backslash can escape any
        // subsequent escape code... except "u".
        if (currentCharacter == 'u' && !isUnicode) {
          // The spec allows multiple "u" after the "\".
          do {
            nextCharacter();
          } while (currentCharacter == 'u');
          currentCharacter = (char) Integer.parseInt(source.substring(position, position + 4), 16);
          position += 3;
          isUnicode = true;
          return;
        }
        // If it's not a Unicode escape, we don't care about what character it actually encodes,
        // so let's just skip the char.
      }
      nextCharacter();
      isUnicode = false;
    }

    private void nextCharacter() {
      ++position;
      if (position < source.length()) {
        currentCharacter = source.charAt(position);
      }
    }

    /** Returns the next character, or {@code 0} if we're at the end of the file. */
    private char peek() {
      return position + 1 < source.length() ? source.charAt(position + 1) : 0;
    }
  }

  private static boolean isBanned(char c) {
    return (c >= 0x20 && c <= 0x7E) || c == 0xA || c == 0xD;
  }
}
