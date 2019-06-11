/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.predicates.TypePredicates.allOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.predicates.TypePredicates.not;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.Optional;

/** Flags calls to {@code toString} on lite protos. */
@BugPattern(
    name = "LiteProtoToString",
    summary =
        "#toString on lite protos will not generate a useful representation of the proto from"
            + " optimized builds. Consider whether using some subset of fields instead would"
            + " provide useful information.",
    severity = WARNING,
    providesFix = NO_FIX)
public final class LiteProtoToString extends AbstractToString {
  private static final TypePredicate IS_LITE_PROTO =
      allOf(
          isDescendantOf("com.google.protobuf.MessageLite"),
          not(isDescendantOf("com.google.protobuf.Message")));

  private static final ImmutableSet<String> VERBOSE_LOGGING =
      ImmutableSet.of("atVerbose", "atFine", "atFinest", "atDebug", "v", "d");

  @Override
  protected TypePredicate typePredicate() {
    return LiteProtoToString::matches;
  }

  private static boolean matches(Type type, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return false;
    }
    if (isVerboseLogMessage(state)) {
      return false;
    }
    return IS_LITE_PROTO.apply(type, state);
  }

  private static boolean isVerboseLogMessage(VisitorState state) {
    return Streams.stream(state.getPath()).anyMatch(LiteProtoToString::isVerboseLogMessage);
  }

  private static boolean isVerboseLogMessage(Tree tree) {
    for (; tree instanceof MethodInvocationTree; tree = getReceiver((MethodInvocationTree) tree)) {
      if (VERBOSE_LOGGING.contains(getSymbol(tree).getSimpleName().toString())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Optional<String> descriptionMessageForDefaultMatch(Type type, VisitorState state) {
    return Optional.of(message());
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    return Optional.empty();
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    return Optional.empty();
  }
}
