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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.parser.UnicodeReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Position.LineMap;
import java.util.HashMap;
import java.util.Map;

/** A utility for tokenizing and preserving comments. */
public class ErrorProneTokens {
  private final int offset;
  private final CommentSavingTokenizer commentSavingTokenizer;
  private final ScannerFactory scannerFactory;
  private final Log log;

  public ErrorProneTokens(String source, Context context) {
    this(source, 0, context);
  }

  public ErrorProneTokens(String source, int offset, Context context) {
    this.offset = offset;
    scannerFactory = ScannerFactory.instance(context);
    log = Log.instance(context);
    char[] buffer = source == null ? new char[] {} : source.toCharArray();
    commentSavingTokenizer = new CommentSavingTokenizer(scannerFactory, buffer, buffer.length);
  }

  public LineMap getLineMap() {
    return commentSavingTokenizer.getLineMap();
  }

  public ImmutableList<ErrorProneToken> getTokens() {
    Log.DiagnosticHandler diagHandler = ErrorProneLog.discardDiagnosticHandler(log);
    try {
      Scanner scanner = new AccessibleScanner(scannerFactory, commentSavingTokenizer);
      ImmutableList.Builder<ErrorProneToken> tokens = ImmutableList.builder();
      do {
        scanner.nextToken();
        Token token = scanner.token();
        tokens.add(
            new ErrorProneToken(
                token, offset, getComments(token, commentSavingTokenizer.comments())));
      } while (scanner.token().kind != TokenKind.EOF);
      return tokens.build();
    } finally {
      log.popDiagnosticHandler(diagHandler);
    }
  }

  private static final ImmutableList<ErrorProneComment> getComments(
      Token token, Map<Comment, ErrorProneComment> comments) {
    if (token.comments == null) {
      return ImmutableList.of();
    }
    // javac stores the comments in reverse declaration order because appending to linked
    // lists is expensive
    return token.comments.stream().map(comments::get).collect(toImmutableList()).reverse();
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

    private final Map<Comment, ErrorProneComment> comments = new HashMap<>();

    CommentSavingTokenizer(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }

    public Map<Comment, ErrorProneComment> comments() {
      return comments;
    }

    @Override
    protected Comment processComment(int pos, int endPos, CommentStyle style) {
      char[] buf = getRawCharactersReflectively(pos, endPos);
      Comment comment = super.processComment(pos, endPos, style);
      AccessibleReader reader = new AccessibleReader(fac, buf, buf.length);
      ErrorProneComment errorProneComment =
          new ErrorProneComment(
              pos,
              endPos,
              /* offset= */ 0,
              () -> new String(reader.getRawCharacters()),
              ErrorProneComment.ErrorProneCommentStyle.from(style));
      comments.put(comment, errorProneComment);
      return comment;
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
