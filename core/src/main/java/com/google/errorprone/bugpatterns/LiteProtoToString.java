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
import static com.google.errorprone.predicates.TypePredicates.isExactType;
import static com.google.errorprone.predicates.TypePredicates.not;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
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
        "toString() on lite protos will not generate a useful representation of the proto from"
            + " optimized builds. Consider whether using some subset of fields instead would"
            + " provide useful information.",
    severity = WARNING,
    providesFix = NO_FIX)
public final class LiteProtoToString extends AbstractToString {
  private static final String LITE_ENUM_MESSAGE =
      "toString() on lite proto enums will generate different representations of the value from"
          + " development and optimized builds. Consider using #getNumber if you only need a"
          + " serialized representation of the value, or #name if you really need the name. Using"
          + " #name will prevent the optimizer stripping out the names of elements, however;"
          + " so do not use if this enum contains strings that should not leak external to Google.";

  private static final TypePredicate IS_LITE_PROTO =
      allOf(
          isDescendantOf("com.google.protobuf.MessageLite"),
          not(isDescendantOf("com.google.protobuf.Message")),
          not(isExactType("com.google.protobuf.UnknownFieldSet")));

  private static final TypePredicate IS_LITE_ENUM =
      allOf(
          isDescendantOf("com.google.protobuf.Internal.EnumLite"),
          not(isDescendantOf("com.google.protobuf.ProtocolMessageEnum")),
          not(isDescendantOf("com.google.protobuf.AbstractMessageLite.InternalOneOfEnum")));

  private static final ImmutableSet<String> METHODS_STRIPPED_BY_OPTIMIZER =
      ImmutableSet.<String>builder()
          .add("atVerbose", "atFine", "atFiner", "atFinest", "atDebug", "atConfig", "atInfo")
          .add("v", "d", "i")
          .build();

  @Override
  protected TypePredicate typePredicate() {
    return LiteProtoToString::matches;
  }

  private static boolean matches(Type type, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return false;
    }
    if (isStrippedLogMessage(state)) {
      return false;
    }
    return IS_LITE_PROTO.apply(type, state) || IS_LITE_ENUM.apply(type, state);
  }

  private static boolean isStrippedLogMessage(VisitorState state) {
    return Streams.stream(state.getPath()).anyMatch(LiteProtoToString::isStrippedLogMessage);
  }

  private static boolean isStrippedLogMessage(Tree tree) {
    for (; tree instanceof MethodInvocationTree; tree = getReceiver((MethodInvocationTree) tree)) {
      if (METHODS_STRIPPED_BY_OPTIMIZER.contains(getSymbol(tree).getSimpleName().toString())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Optional<String> descriptionMessageForDefaultMatch(Type type, VisitorState state) {
    return Optional.of(IS_LITE_ENUM.apply(type, state) ? LITE_ENUM_MESSAGE : message());
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    return IS_LITE_ENUM.apply(getType(tree), state)
        ? Optional.of(SuggestedFix.postfixWith(tree, ".getNumber()"))
        : Optional.empty();
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    return IS_LITE_ENUM.apply(getType(tree), state)
        ? Optional.of(
            SuggestedFix.replace(
                parent, String.format("%s.getNumber()", state.getSourceForNode(tree))))
        : Optional.empty();
  }
}
