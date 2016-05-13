/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;

@BugPattern(
  category = ONE_OFF,
  maturity = EXPERIMENTAL,
  name = "ProtoStringFieldReferenceEquality",
  severity = ERROR,
  summary = "Comparing protobuf fields of type String using reference equality",
  explanation =
      "Comparing strings with == is almost always an error, but it is an error 100% "
          + "of the time when one of the strings is a protobuf field.  Additionally, protobuf "
          + "fields cannot be null, so Object.equals(Object) is always more correct."
)
public class ProtoStringFieldReferenceEquality extends AbstractReferenceEquality {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final Matcher<ExpressionTree> PROTO_STRING_METHOD =
      allOf(instanceMethod().onDescendantOf(PROTO_SUPER_CLASS), isSameType("java.lang.String"));

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    return PROTO_STRING_METHOD.matches(tree, state);
  }
}
