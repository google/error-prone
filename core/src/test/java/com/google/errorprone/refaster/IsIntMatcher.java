/*
 * Copyright 2026 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeKind;

/** Test matcher that matches expressions whose static type is exactly primitive int. */
public final class IsIntMatcher implements Matcher<ExpressionTree> {
  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    return type != null && type.getKind() == TypeKind.INT;
  }
}
