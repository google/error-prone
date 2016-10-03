/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import java.util.regex.Pattern;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author pepstein@google.com (Peter Epstein)
 */
public class StringLiteral implements Matcher<ExpressionTree> {

  private final Pattern pattern;

  public StringLiteral(String value) {
    this.pattern = Pattern.compile(value, Pattern.LITERAL);
  }

  public StringLiteral(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(ExpressionTree expressionTree, VisitorState state) {
    if (expressionTree instanceof LiteralTree) {
      LiteralTree literalTree = (LiteralTree) expressionTree;
      Object actualValue = literalTree.getValue();
      return actualValue instanceof String && pattern.matcher((String) actualValue).matches();
    } else {
      return false;
    }
  }
}
