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

import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the corrected source which we think was intended, by
 * applying a SuggestedFix.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class AppliedFix {
  private final CharSequence newSource;
  private final String snippet;
  private final boolean isRemoveLine;

  private AppliedFix(CharSequence newSource, String snippet, boolean isRemoveLine) {
    this.newSource = newSource;
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
    private final Map<JCTree, Integer> endPositions;

    public Applier(CharSequence source, Map<JCTree, Integer> endPositions) {
      this.source = source;
      this.endPositions = endPositions;
    }

    public AppliedFix apply(SuggestedFix suggestedFix) {
      StringBuilder replaced = new StringBuilder(source);

      Set<Integer> modifiedLines = new HashSet<Integer>();
      for (Replacement repl : suggestedFix.getReplacements(endPositions)) {
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

      // TODO: Not sure this is really the right behavior, but otherwise we can end up with an
      // infinite loop below.
      if (modifiedLines.isEmpty()) {
        return null;
      }

      LineNumberReader lineNumberReader =
              new LineNumberReader(new StringReader(replaced.toString()));
      String snippet = null;
      boolean isRemoveLine = false;
      try {
        while(!modifiedLines.contains(lineNumberReader.getLineNumber())) {
          lineNumberReader.readLine();
        }
        // TODO: this is over-simplified; need a failing test case
        snippet = lineNumberReader.readLine().trim();
        // snip comment from line
        if (snippet.contains("//")) {
          snippet = snippet.substring(0, snippet.indexOf("//")).trim();
        }
        if (snippet.isEmpty()) {
          isRemoveLine = true;
          snippet = "to remove this line";
        }
      } catch (IOException e) {
        // impossible since source is in-memory
      }
      return new AppliedFix(replaced, snippet, isRemoveLine);
    }
  }

  public static Applier fromSource(CharSequence source, Map<JCTree, Integer> endPositions) {
    return new Applier(source, endPositions);
  }
}
