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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.regex.Pattern;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "Setting a proto field to an UNRECOGNIZED value will result in an exception at runtime when"
            + " building.",
    severity = ERROR)
public final class SetUnrecognized extends BugChecker implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!IS_PROTO_SETTER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getArguments().size() != 1) {
      return NO_MATCH;
    }
    ExpressionTree arg = tree.getArguments().get(0);
    var argSymbol = getSymbol(arg);
    if (argSymbol == null) {
      return NO_MATCH;
    }
    if (!argSymbol.getSimpleName().contentEquals("UNRECOGNIZED")) {
      return NO_MATCH;
    }
    if (!isSubtype(argSymbol.owner.type, ENUM_LITE.get(state), state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static final Matcher<ExpressionTree> IS_PROTO_SETTER =
      instanceMethod()
          .onDescendantOf("com.google.protobuf.MessageLite.Builder")
          .withNameMatching(Pattern.compile("(add|set).*"));

  private static final Supplier<Type> ENUM_LITE =
      VisitorState.memoize(
          state -> state.getTypeFromString("com.google.protobuf.Internal.EnumLite"));
}
