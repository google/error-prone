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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;

/** Bans, without the possibility of suppression, the use of direction-changing Unicode escapes. */
@BugPattern(
    name = "UnicodeDirectionalityCharacters",
    severity = ERROR,
    summary = "Unicode directionality modifiers can be used to conceal code in many editors.",
    disableable = false)
public final class UnicodeDirectionalityCharacters extends BugChecker
    implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    CharSequence source = state.getSourceCode();

    for (int i = 0; i < source.length(); ++i) {
      char c = source.charAt(i);
      if (isDangerous(c)) {
        state.reportMatch(
            describeMatch(
                new FixedPosition(tree, i),
                SuggestedFix.replace(i, i + 1, String.format("\\u%04x", (int) c))));
      }
    }
    return NO_MATCH;
  }

  private static boolean isDangerous(char c) {
    switch (c) {
      case 0x202A: // Left-to-Right Embedding
      case 0x202B: // Right-to-Left Embedding
      case 0x202C: // Pop Directional Formatting
      case 0x202D: // Left-to-Right Override
      case 0x202E: // Right-to-Left Override
      case 0x2066: // Left-to-Right Isolate
      case 0x2067: // Right-to-Left Isolate
      case 0x2068: // First Strong Isolate
      case 0x2069: // Pop Directional Isolate
        return true;
      default:
        return false;
    }
  }
}
