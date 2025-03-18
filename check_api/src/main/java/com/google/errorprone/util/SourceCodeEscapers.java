/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.ArrayBasedCharEscaper;
import com.google.common.escape.CharEscaper;
import com.google.common.escape.Escaper;
import java.util.Map;

/**
 * A factory for Escaper instances used to escape strings for safe use in Java.
 *
 * <p>This is a subset of source code escapers that are in the process of being open-sources as part
 * of guava, see: https://github.com/google/guava/issues/1620
 */
// TODO(cushon): migrate to the guava version once it is open-sourced, and delete this
public final class SourceCodeEscapers {
  private SourceCodeEscapers() {}

  // For each xxxEscaper() method, please add links to external reference pages
  // that are considered authoritative for the behavior of that escaper.

  // From: https://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters
  private static final char PRINTABLE_ASCII_MIN = 0x20; // ' '
  private static final char PRINTABLE_ASCII_MAX = 0x7E; // '~'

  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  /**
   * Returns an {@link Escaper} instance that escapes special characters in a string so it can
   * safely be included in either a Java character literal or string literal. This is the preferred
   * way to escape Java characters for use in String or character literals.
   *
   * <p>See: <a href= "https://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089"
   * >The Java Language Specification</a> for more details.
   */
  public static CharEscaper javaCharEscaper() {
    return JAVA_CHAR_ESCAPER;
  }

  private static final CharEscaper JAVA_CHAR_ESCAPER =
      new JavaCharEscaper(
          ImmutableMap.of(
              '\b', "\\b",
              '\f', "\\f",
              '\n', "\\n",
              '\r', "\\r",
              '\t', "\\t",
              '\"', "\\\"",
              '\\', "\\\\",
              '\'', "\\'"));

  /**
   * Returns an {@link Escaper} instance that escapes special characters in a string so it can
   * safely be included in either a Java text block.
   *
   * <p>Double quotes are not escaped. If the string contents contain a run of three or more
   * consecutive double quotes, additional action is required to make it safe to include in a text
   * block.
   *
   * <p>See: <a href= "https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.6"
   * >The Java Language Specification</a> for more details.
   *
   * <p>The Guava FR for this is: https://github.com/google/guava/issues/7421
   */
  public static CharEscaper getJavaTextBlockEscaper() {
    return JAVA_TEXT_BLOCK_ESCAPER;
  }

  private static final CharEscaper JAVA_TEXT_BLOCK_ESCAPER =
      new JavaCharEscaper(
          ImmutableMap.of(
              '\b', "\\b",
              '\f', "\\f",
              '\n', "\\n",
              '\r', "\\r",
              '\t', "\\t",
              '\\', "\\\\"));

  // This escaper does not produce octal escape sequences. See:
  // https://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089
  //  "Octal escapes are provided for compatibility with C, but can express
  //   only Unicode values \u0000 through \u00FF, so Unicode escapes are
  //   usually preferred."
  private static class JavaCharEscaper extends ArrayBasedCharEscaper {
    JavaCharEscaper(Map<Character, String> replacements) {
      super(replacements, PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX);
    }

    @Override
    protected char[] escapeUnsafe(char c) {
      return asUnicodeHexEscape(c);
    }
  }

  // Helper for common case of escaping a single char.
  private static char[] asUnicodeHexEscape(char c) {
    // Equivalent to String.format("\\u%04x", (int)c);
    char[] r = new char[6];
    r[0] = '\\';
    r[1] = 'u';
    r[5] = HEX_DIGITS[c & 0xF];
    c = (char) (c >>> 4);
    r[4] = HEX_DIGITS[c & 0xF];
    c = (char) (c >>> 4);
    r[3] = HEX_DIGITS[c & 0xF];
    c = (char) (c >>> 4);
    r[2] = HEX_DIGITS[c & 0xF];
    return r;
  }
}
