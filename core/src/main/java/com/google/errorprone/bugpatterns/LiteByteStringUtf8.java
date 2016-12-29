/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.toType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;

/** @author glorioso@google.com (Nick Glorioso) */
@BugPattern(
  name = "LiteByteStringUtf8",
  category = PROTOBUF,
  summary =
      "This pattern will silently corrupt certain byte sequences from the serialized protocol "
          + "message. Use ByteString or byte[] directly",
  severity = ERROR
)
public class LiteByteStringUtf8 extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> BYTE_STRING_UTF_8 =
      allOf(
          instanceMethod().onDescendantOf("com.google.protobuf.ByteString").named("toStringUtf8"),
          receiverOfInvocation(
              toType(
                  MethodInvocationTree.class,
                  instanceMethod()
                      .onDescendantOf("com.google.protobuf.MessageLite")
                      .named("toByteString"))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return BYTE_STRING_UTF_8.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
