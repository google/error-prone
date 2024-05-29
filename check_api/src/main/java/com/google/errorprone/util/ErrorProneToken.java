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
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Name;

/** Wraps a javac {@link Token} to return comments in declaration order. */
public class ErrorProneToken {
  private final Token token;
  private final int offset;
  private final ImmutableList<ErrorProneComment> comments;

  ErrorProneToken(Token token, int offset, ImmutableList<ErrorProneComment> comments) {
    this.token = token;
    this.offset = offset;
    this.comments = comments;
  }

  public TokenKind kind() {
    return token.kind;
  }

  public int pos() {
    return offset + token.pos;
  }

  public int endPos() {
    return offset + token.endPos;
  }

  public ImmutableList<ErrorProneComment> comments() {
    return comments.stream().map(c -> c.withOffset(offset)).collect(toImmutableList());
  }

  public boolean hasName() {
    // the subclasses of Token jealously guard their secrets;
    // inspect class names to figure out which hazzers are supported by a given token
    return token.getClass().getSimpleName().contentEquals("NamedToken");
  }

  public boolean hasStringVal() {
    String name = token.getClass().getSimpleName();
    return name.contentEquals("StringToken") || name.contentEquals("NumericToken");
  }

  public boolean hasRadix() {
    return token.getClass().getSimpleName().contentEquals("NumericToken");
  }

  public Name name() {
    return token.name();
  }

  public String stringVal() {
    return token.stringVal();
  }

  public int radix() {
    return token.radix();
  }

  @Override
  public String toString() {
    return token.toString();
  }
}
