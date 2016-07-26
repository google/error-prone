/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.common.collect.Lists;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.List;

/** Wraps a javac {@link Token} to return comments in declaration order. */
public class ErrorProneToken {

  private final Token token;

  ErrorProneToken(Token token) {
    this.token = token;
  }

  public TokenKind kind() {
    return token.kind;
  }

  public int pos() {
    return token.pos;
  }

  public int endPos() {
    return token.endPos;
  }

  public List<Comment> comments() {
    // javac stores the comments in reverse declaration order because appending to linked
    // lists is expensive
    return token.comments == null
        ? Collections.<Comment>emptyList()
        : Lists.reverse(token.comments);
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
