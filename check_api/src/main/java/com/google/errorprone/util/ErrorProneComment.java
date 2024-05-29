/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;

/** Wraps a {@link Comment} to allow an offset source position to be reported. */
public final class ErrorProneComment {

  private final int pos;
  private final int endPos;
  private final int offset;
  private final Supplier<String> text;
  private final CommentStyle style;

  ErrorProneComment(int pos, int endPos, int offset, Supplier<String> text, CommentStyle style) {
    this.pos = pos;
    this.endPos = endPos;
    this.offset = offset;
    this.text = Suppliers.memoize(text);
    this.style = style;
  }

  public ErrorProneComment withOffset(int offset) {
    return new ErrorProneComment(pos, endPos, offset, text, style);
  }

  public int getPos() {
    return pos + offset;
  }

  public int getEndPos() {
    return endPos + offset;
  }

  /**
   * Returns the source position of the character at index {@code index} in the comment text.
   *
   * <p>The handling of javadoc comments in javac has more logic to skip over leading whitespace and
   * '*' characters when indexing into doc comments, but we don't need any of that.
   */
  public int getSourcePos(int index) {
    checkArgument(
        0 <= index && index < (endPos - pos),
        "Expected %s in the range [0, %s)",
        index,
        endPos - pos);
    return pos + index + offset;
  }

  public CommentStyle getStyle() {
    return style;
  }

  public String getText() {
    return text.get();
  }

  /**
   * We don't care about {@code @deprecated} javadoc tags (see the DepAnn check).
   *
   * @return false
   */
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public String toString() {
    return String.format("Comment: '%s'", getText());
  }
}
