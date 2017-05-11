/*
 * Copyright 2011 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.apply;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.io.CharSource;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;

/**
 * Representation of a mutable Java source file.
 *
 * <p>This class is not thread-safe.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SourceFile {

  private final String path;
  private final StringBuilder sourceBuilder;

  public static SourceFile create(JavaFileObject fileObject) throws IOException {
    return new SourceFile(fileObject.toUri().getPath(), fileObject.getCharContent(false));
  }

  public SourceFile(String path, CharSequence source) {
    this.path = path;
    sourceBuilder = new StringBuilder(source);
  }

  /** Returns the path for this source file */
  public String getPath() {
    return path;
  }

  /** Returns a copy of code as a list of lines. */
  public List<String> getLines() {
    try {
      return CharSource.wrap(sourceBuilder).readLines();
    } catch (IOException e) {
      throw new AssertionError("IOException not possible, as the string is in-memory");
    }
  }

  /** Returns a copy of the code as a string. */
  public String getSourceText() {
    return sourceBuilder.toString();
  }

  public CharSequence getAsSequence() {
    return CharBuffer.wrap(sourceBuilder).asReadOnlyBuffer();
  }

  /** Clears the current source test for this SourceFile and resets it to the passed-in value. */
  public void setSourceText(CharSequence source) {
    sourceBuilder.setLength(0); // clear StringBuilder
    sourceBuilder.append(source);
  }

  /**
   * Returns a fragment of the source code as a string.
   *
   * <p>This method uses the same conventions as {@link String#substring(int, int)} for its start
   * and end parameters.
   */
  public String getFragmentByChars(int startPosition, int endPosition) {
    return sourceBuilder.substring(startPosition, endPosition);
  }

  /**
   * Returns a fragment of the source code between the two stated line numbers. The parameters
   * represent <b>inclusive</b> line numbers.
   *
   * <p>The returned fragment will end in a newline.
   */
  public String getFragmentByLines(int startLine, int endLine) {
    Preconditions.checkArgument(startLine <= endLine);
    return Joiner.on("\n").join(getLines(startLine, endLine)) + "\n";
  }

  private List<String> getLines(int startLine, int endLine) {
    LineNumberReader reader = new LineNumberReader(new StringReader(sourceBuilder.toString()));
    List<String> lines = new ArrayList<>(endLine - startLine + 1);
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        if (reader.getLineNumber() >= startLine) {
          lines.add(line);
        }
        if (reader.getLineNumber() >= endLine) {
          break;
        }
      }
      return lines;
    } catch (IOException e) {
      throw new AssertionError("Wrapped StringReader should not produce I/O exceptions");
    }
  }

  /** Replace the source code with the new lines of code. */
  public void replaceLines(List<String> lines) {
    sourceBuilder.replace(0, sourceBuilder.length(), Joiner.on("\n").join(lines) + "\n");
  }

  /** Replace the source code between the start and end lines with some new lines of code. */
  public void replaceLines(int startLine, int endLine, List<String> replacementLines) {
    Preconditions.checkArgument(startLine <= endLine);
    List<String> originalLines = getLines();
    List<String> newLines = new ArrayList<>();
    for (int i = 0; i < originalLines.size(); i++) {
      int lineNum = i + 1;
      if (lineNum == startLine) {
        newLines.addAll(replacementLines);
      } else if (lineNum > startLine && lineNum <= endLine) {
        // Skip
      } else {
        newLines.add(originalLines.get(i));
      }
    }
    replaceLines(newLines);
  }

  /**
   * Replace the source code between the start and end character positions with a new string.
   *
   * <p>This method uses the same conventions as {@link String#substring(int, int)} for its start
   * and end parameters.
   */
  public void replaceChars(int startPosition, int endPosition, String replacement) {
    try {
      sourceBuilder.replace(startPosition, endPosition, replacement);
    } catch (StringIndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException(
          String.format(
              "Replacement cannot be made. Source file %s has length %d, requested start "
                  + "position %d, requested end position %d, replacement %s",
              path, sourceBuilder.length(), startPosition, endPosition, replacement));
    }
  }
}
