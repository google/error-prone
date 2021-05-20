/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** Analysis helpers for flogger. */
final class FloggerHelpers {

  private static final char STRING_FORMAT = 's';

  static char inferFormatSpecifier(Tree piece, VisitorState state) {
    Type type = getType(piece);
    return inferFormatSpecifier(type, state);
  }

  static char inferFormatSpecifier(Type type, VisitorState state) {
    if (type == null) {
      return STRING_FORMAT;
    }
    switch (state.getTypes().unboxedTypeOrType(type).getKind()) {
      case INT:
      case LONG:
        return 'd';
      case FLOAT:
      case DOUBLE:
        return 'g';
      case BOOLEAN:
        // %b is identical to %s in Flogger, but not in String.format, so it might be risky
        // to train people to prefer it. (In format() a Boolean "null" becomes "false",)
        return STRING_FORMAT;
      default:
        return STRING_FORMAT;
    }
  }

  private FloggerHelpers() {}
}
