/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Optional;

/** Flags ignored return values from pure getters. */
@BugPattern(
    severity = ERROR,
    summary =
        "Getters on AutoValues, AutoBuilders, and Protobuf Messages are side-effect free, so there"
            + " is no point in calling them if the return value is ignored. While there are no"
            + " side effects from the getter, the receiver may have side effects.")
public final class IgnoredPureGetter extends AbstractReturnValueIgnored {

  private static final Supplier<Type> MESSAGE_LITE =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.protobuf.MessageLite"));

  private static final Supplier<Type> MUTABLE_MESSAGE_LITE =
      VisitorState.memoize(
          state -> state.getTypeFromString("com.google.protobuf.MutableMessageLite"));

  private final boolean checkAllProtos;
  private final boolean checkAutoBuilders;

  public IgnoredPureGetter() {
    this(ErrorProneFlags.empty());
  }

  public IgnoredPureGetter(ErrorProneFlags flags) {
    super(flags);
    this.checkAllProtos = flags.getBoolean("IgnoredPureGetter:CheckAllProtos").orElse(true);
    this.checkAutoBuilders = flags.getBoolean("IgnoredPureGetter:CheckAutoBuilders").orElse(true);
  }

  @Override
  protected Matcher<? super ExpressionTree> specializedMatcher() {
    return this::isPureGetter;
  }

  @Override
  public ImmutableMap<String, ?> getMatchMetadata(ExpressionTree tree, VisitorState state) {
    return pureGetterKind(tree, state)
        .map(kind -> ImmutableMap.of("pure_getter_kind", kind))
        .orElse(ImmutableMap.of());
  }

  @Override
  protected Description describeReturnValueIgnored(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    Description.Builder builder =
        buildDescription(methodInvocationTree)
            .addFix(
                SuggestedFix.builder()
                    .setShortDescription("Remove with any side effects from the receiver")
                    .delete(
                        parent instanceof ExpressionStatementTree ? parent : methodInvocationTree)
                    .build());
    ExpressionTree receiver = getReceiver(methodInvocationTree);
    if (receiver instanceof MethodInvocationTree) {
      builder.addFix(
          SuggestedFix.builder()
              .setShortDescription("Remove but keep side effects from the receiver")
              .replace(methodInvocationTree, state.getSourceForNode(receiver))
              .build());
    }
    return builder.build();
  }

  // TODO(b/222475003): make this static again once the flag is gone
  private boolean isPureGetter(ExpressionTree tree, VisitorState state) {
    return pureGetterKind(tree, state).isPresent();
  }

  // TODO(b/222475003): make this static again once the flag is gone
  private Optional<PureGetterKind> pureGetterKind(ExpressionTree tree, VisitorState state) {
    Symbol rawSymbol = getSymbol(tree);
    if (!(rawSymbol instanceof MethodSymbol)) {
      return Optional.empty();
    }
    MethodSymbol symbol = (MethodSymbol) rawSymbol;
    Symbol owner = symbol.owner;

    if (symbol.getModifiers().contains(ABSTRACT) && symbol.getParameters().isEmpty()) {
      // The return value of any abstract method on an @AutoValue needs to be used.
      if (hasAnnotation(owner, "com.google.auto.value.AutoValue", state)) {
        return Optional.of(PureGetterKind.AUTO_VALUE);
      }
      // The return value of any abstract method on an @AutoBuilder (which doesn't return the
      // Builder itself) needs to be used.
      if (checkAutoBuilders
          && hasAnnotation(owner, "com.google.auto.value.AutoBuilder", state)
          && !isSameType(symbol.getReturnType(), owner.type, state)) {
        return Optional.of(PureGetterKind.AUTO_BUILDER);
      }
      // The return value of any abstract method on an @AutoValue.Builder (which doesn't return the
      // Builder itself) needs to be used.
      if (hasAnnotation(owner, "com.google.auto.value.AutoValue.Builder", state)
          && !isSameType(symbol.getReturnType(), owner.type, state)) {
        return Optional.of(PureGetterKind.AUTO_VALUE_BUILDER);
      }
    }

    try {
      if (isSubtype(owner.type, MESSAGE_LITE.get(state), state)
          && !isSubtype(owner.type, MUTABLE_MESSAGE_LITE.get(state), state)) {
        String name = symbol.getSimpleName().toString();
        if ((name.startsWith("get") || name.startsWith("has"))
            && symbol.getParameters().isEmpty()) {
          return Optional.of(PureGetterKind.PROTO);
        }
        if (checkAllProtos) {
          return Optional.of(PureGetterKind.PROTO);
        }
      }
    } catch (Symbol.CompletionFailure ignore) {
      // isSubtype may throw this if some supertype's class file isn't found
      // Nothing we can do about it as far as I know
    }
    return Optional.empty();
  }

  private enum PureGetterKind {
    AUTO_VALUE,
    AUTO_VALUE_BUILDER,
    AUTO_BUILDER,
    PROTO
  }
}
