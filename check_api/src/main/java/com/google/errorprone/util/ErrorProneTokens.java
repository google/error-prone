/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.parser.UnicodeReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Position.LineMap;

/** A utility for tokenizing and preserving comments. */
public class ErrorProneTokens {
  private final int offset;
  private final CommentSavingTokenizer commentSavingTokenizer;
  private final ScannerFactory scannerFactory;

  public ErrorProneTokens(String source, Context context) {
    this(source, 0, context);
  }

  public ErrorProneTokens(String source, int offset, Context context) {
    this.offset = offset;
    scannerFactory = ScannerFactory.instance(context);
    char[] buffer = source == null ? new char[] {} : source.toCharArray();
    commentSavingTokenizer = new CommentSavingTokenizer(scannerFactory, buffer, buffer.length);
  }

  public LineMap getLineMap() {
    return commentSavingTokenizer.getLineMap();
  }

  public ImmutableList<ErrorProneToken> getTokens() {
    Scanner scanner = new AccessibleScanner(scannerFactory, commentSavingTokenizer);
    ImmutableList.Builder<ErrorProneToken> tokens = ImmutableList.builder();
    do {
      scanner.nextToken();
      tokens.add(new ErrorProneToken(scanner.token(), offset));
    } while (scanner.token().kind != TokenKind.EOF);
    return tokens.build();
  }

  /** Returns the tokens for the given source text, including comments. */
  public static ImmutableList<ErrorProneToken> getTokens(String source, Context context) {
    return getTokens(source, 0, context);
  }

  /**
   * Returns the tokens for the given source text, including comments, indicating the offset of the
   * source within the overall file.
   */
  public static ImmutableList<ErrorProneToken> getTokens(
      String source, int offset, Context context) {
    return new ErrorProneTokens(source, offset, context).getTokens();
  }

  /** A {@link JavaTokenizer} that saves comments. */
  static class CommentSavingTokenizer extends JavaTokenizer {
    CommentSavingTokenizer(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }

    @Override
    protected Comment processComment(int pos, int endPos, CommentStyle style) {
      char[] buf = getRawCharactersReflectively(pos, endPos);
      return new CommentWithTextAndPosition(
          pos, endPos, new AccessibleReader(fac, buf, buf.length), style);
    }

    private char[] getRawCharactersReflectively(int beginIndex, int endIndex) {
      Object instance;
      try {
        instance = JavaTokenizer.class.getDeclaredField("reader").get(this);
      } catch (ReflectiveOperationException e) {
        instance = this;
      }
      try {
        return (char[])
            instance
                .getClass()
                .getMethod("getRawCharacters", int.class, int.class)
                .invoke(instance, beginIndex, endIndex);
      } catch (ReflectiveOperationException e) {
        throw new LinkageError(e.getMessage(), e);
      }
    }
  }

  /** A {@link Comment} that saves its text and start position. */
  static class CommentWithTextAndPosition implements Comment {

    private final int pos;
    private final int endPos;
    private final AccessibleReader reader;
    private final CommentStyle style;

    private String text = null;

    public CommentWithTextAndPosition(
        int pos, int endPos, AccessibleReader reader, CommentStyle style) {
      this.pos = pos;
      this.endPos = endPos;
      this.reader = reader;
      this.style = style;
    }

    public int getPos() {
      return pos;
    }

    public int getEndPos() {
      return endPos;
    }

    /**
     * Returns the source position of the character at index {@code index} in the comment text.
     *
     * <p>The handling of javadoc comments in javac has more logic to skip over leading whitespace
     * and '*' characters when indexing into doc comments, but we don't need any of that.
     */
    @Override
    public int getSourcePos(int index) {
      checkArgument(
          0 <= index && index < (endPos - pos),
          "Expected %s in the range [0, %s)",
          index,
          endPos - pos);
      return pos + index;
    }

    @Override
    public CommentStyle getStyle() {
      return style;
    }

    @Override
    public String getText() {
      String text = this.text;
      if (text == null) {
        this.text = text = new String(reader.getRawCharacters());
      }
      return text;
    }

    /**
     * We don't care about {@code @deprecated} javadoc tags (see the DepAnn check).
     *
     * @return false
     */
    @Override
    public boolean isDeprecated() {
      return false;
    }

    @Override
    public String toString() {
      return String.format("Comment: '%s'", getText());
    }
  }

  // Scanner(ScannerFactory, JavaTokenizer) is package-private
  static class AccessibleScanner extends Scanner {
    protected AccessibleScanner(ScannerFactory fac, JavaTokenizer tokenizer) {
      super(fac, tokenizer);
    }
  }

  // UnicodeReader(ScannerFactory, char[], int) is package-private
  static class AccessibleReader extends UnicodeReader {
    protected AccessibleReader(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }
  }
}
