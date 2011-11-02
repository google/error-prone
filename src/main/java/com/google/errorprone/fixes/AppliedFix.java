/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the corrected source which we think was intended, by
 * applying a SuggestedFix.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class AppliedFix {
  private final CharSequence newSource;
  private final String snippet;

  public AppliedFix(CharSequence newSource, String snippet) {
    this.newSource = newSource;
    this.snippet = snippet;
  }

  public CharSequence getNewCodeSnippet() {
    return snippet;
  }

  public static class Applier {
    private CharSequence source;

    public Applier(CharSequence source) {
      this.source = source;
    }

    public AppliedFix apply(SuggestedFix suggestedFix) {
      StringBuilder replaced = new StringBuilder(source);

      Set<Integer> modifiedLines = new HashSet<Integer>();
      for (Replacement repl : suggestedFix.getReplacements()) {
        replaced.replace(repl.startPosition, repl.endPosition, repl.replaceWith);

        // Find the line number(s) being modified
        // TODO: this could be more efficient
        try {
          LineNumberReader lineNumberReader =
              new LineNumberReader(new StringReader(source.toString()));
          lineNumberReader.skip(repl.startPosition);
          modifiedLines.add(lineNumberReader.getLineNumber());
        } catch (IOException e) {
          // impossible since source is in-memory
        }
      }

      LineNumberReader lineNumberReader =
              new LineNumberReader(new StringReader(replaced.toString()));
      String snippet = null;
      try {
        while(!modifiedLines.contains(lineNumberReader.getLineNumber())) {
          lineNumberReader.readLine();
        }
        // TODO: this is over-simplified; need a failing test case
        snippet = lineNumberReader.readLine().trim();
        if (snippet.isEmpty()) {
          snippet = "to remove this line";
        }
      } catch (IOException e) {
        // impossible since source is in-memory
      }
      return new AppliedFix(replaced, snippet);
    }
  }

  public static Applier fromSource(CharSequence source) {
    return new Applier(source);
  }
}
