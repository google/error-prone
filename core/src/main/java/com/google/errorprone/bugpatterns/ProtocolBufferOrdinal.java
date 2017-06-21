/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.PROTOBUF;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Points out if #ordinal() is called on a Protocol Buffer Enum.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
  name = "ProtocolBufferOrdinal",
  summary = "To get the tag number of a protocol buffer enum, use getNumber() instead.",
  category = PROTOBUF,
  severity = ERROR
)
public class ProtocolBufferOrdinal extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.Internal.EnumLite";

  private static final Matcher<ExpressionTree> PROTO_MSG_ORDINAL_MATCHER =
      instanceMethod().onDescendantOf(PROTO_SUPER_CLASS).named("ordinal").withParameters();

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    return PROTO_MSG_ORDINAL_MATCHER.matches(methodInvocationTree, state)
        ? describeMatch(methodInvocationTree)
        : Description.NO_MATCH;
  }
}
