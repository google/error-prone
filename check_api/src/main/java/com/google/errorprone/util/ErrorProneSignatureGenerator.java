/*
 * Copyright 2024 The Error Prone Authors.
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

import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Name;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

class ErrorProneSignatureGenerator extends Types.SignatureGenerator {

  private final com.sun.tools.javac.util.ByteBuffer buffer =
      new com.sun.tools.javac.util.ByteBuffer();

  protected ErrorProneSignatureGenerator(Types types) {
    super(types);
  }

  @Override
  protected void append(char ch) {
    buffer.appendByte(ch);
  }

  @Override
  protected void append(byte[] ba) {
    buffer.appendBytes(ba);
  }

  @Override
  protected void append(Name name) {
    buffer.appendName(name);
  }

  @Override
  public String toString() {
    // We could use buffer.toName(Names), but we want a string anyways and this
    // avoids plumbing a Context or instances of Names through.
    // Names always uses UTF-8 internally.
    return new String(Arrays.copyOf(buffer.elems, buffer.length), UTF_8);
  }
}
