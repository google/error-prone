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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "OptionalEquality",
  summary = "Comparison using reference equality instead of value equality",
  explanation =
      "Optionals should be compared for value equality using `.equals()`, and not for reference "
          + "equality using `==` and `!=`.",
  category = GUAVA,
  severity = ERROR
)
public class OptionalEquality extends AbstractReferenceEquality {

  private static final ImmutableSet<String> OPTIONAL_CLASSES =
      ImmutableSet.of(com.google.common.base.Optional.class.getName(), "java.util.Optional");

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    for (String className : OPTIONAL_CLASSES) {
      if (ASTHelpers.isSameType(type, state.getTypeFromString(className), state)) {
        return true;
      }
    }
    return false;
  }
}
