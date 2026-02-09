/*
 * Copyright 2026 The Error Prone Authors.
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

import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.util.Context;

/** A compatibility wrapper around {@link ParserFactory}. */
public final class ErrorProneParser {

  public static JavacParser newParser(
      Context context,
      CharSequence source,
      boolean keepDocComments,
      boolean keepEndPos,
      boolean keepLineMap) {
    ParserFactory parserFactory = ParserFactory.instance(context);
    if (IS_END_POS_TABLE_PRESENT) {
      return parserFactory.newParser(source, keepDocComments, keepEndPos, keepLineMap);
    }
    return parserFactory.newParser(
        source, keepDocComments, keepLineMap, /* parseModuleInfo */ false);
  }

  private static final boolean IS_END_POS_TABLE_PRESENT = getIsEndPosTablePresent();

  private static boolean getIsEndPosTablePresent() {
    try {
      // JDK versions before https://bugs.openjdk.org/browse/JDK-8372948
      Class.forName("com.sun.tools.javac.tree.EndPosTable");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private ErrorProneParser() {}
}
