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

package com.google.errorprone;

import com.sun.tools.javac.util.Convert;

/**
 * @see VisitorState#getConstantExpression(Object)
 */
final class ConstantStringExpressions {
  private ConstantStringExpressions() {}

  static String toConstantStringExpression(Object value, VisitorState state) {
    if (!(value instanceof CharSequence)) {
      return state.getElements().getConstantExpression(value);
    }

    // Don't escape single-quotes in string literals.
    CharSequence str = (CharSequence) value;
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '\'') {
        sb.append('\'');
      } else {
        sb.append(Convert.quote(c));
      }
    }
    return sb.append('"').toString();
  }
}
