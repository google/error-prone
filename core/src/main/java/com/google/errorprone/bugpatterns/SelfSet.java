/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;
import javax.inject.Inject;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary = "This setter seems to be invoked with a value from its own getter. Is it redundant?")
public final class SelfSet extends BugChecker implements MethodInvocationTreeMatcher {
  private final ConstantExpressions constantExpressions;

  @Inject
  SelfSet(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!PROTO_SETTER.matches(tree, state) || tree.getArguments().size() != 1) {
      return NO_MATCH;
    }
    var argument = getOnlyElement(tree.getArguments());
    // TODO(ghm): Consider broadening to more than protos? AutoValues are at least very predictable,
    // as is record + AutoBuilder. We could also just try not restricting at all.
    if (!PROTO_GETTER.matches(argument, state)) {
      return NO_MATCH;
    }
    String getterName = getSymbol(tree).getSimpleName().toString().replaceFirst("^set", "get");
    if (!getSymbol(argument).getSimpleName().contentEquals(getterName)) {
      return NO_MATCH;
    }
    var setterReceiver = getReceiver(tree);
    var getterReceiver = getReceiver(argument);
    if (setterReceiver == null || getterReceiver == null) {
      return NO_MATCH;
    }
    if (!constantExpressions.isSame(setterReceiver, getterReceiver, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static final Matcher<ExpressionTree> PROTO_SETTER =
      instanceMethod()
          .onDescendantOf("com.google.protobuf.MessageLite.Builder")
          .withNameMatching(Pattern.compile("set.*"));

  private static final Matcher<ExpressionTree> PROTO_GETTER =
      instanceMethod()
          .onDescendantOf("com.google.protobuf.MessageLiteOrBuilder")
          .withNameMatching(Pattern.compile("get.*"));
}
