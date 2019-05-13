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

import com.sun.tools.javac.parser.Tokens.Comment;

/** Wraps a {@link Comment} to allow an offset source position to be reported. */
final class OffsetComment implements Comment {

  private final Comment wrapped;
  private final int offset;

  OffsetComment(Comment wrapped, int offset) {
    this.wrapped = wrapped;
    this.offset = offset;
  }

  @Override
  public String getText() {
    return wrapped.getText();
  }

  @Override
  public int getSourcePos(int i) {
    return wrapped.getSourcePos(i) + offset;
  }

  @Override
  public CommentStyle getStyle() {
    return wrapped.getStyle();
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }
}
