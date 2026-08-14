/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** Checks for reliance on identity semantics of value-based classes. */
@BugPattern(
    summary =
        "Value-based classes do not have identity; relying on identity semantics (such as"
            + " WeakReference, IdentityHashMap, synchronization, or identity-based caches) is"
            + " unsafe and will fail in a future version of Java.",
    severity = ERROR)
public class ValueClassIdentity extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher, SynchronizedTreeMatcher {

  private static final Supplier<Type> REFERENCE_TYPE =
      Suppliers.typeFromString("java.lang.ref.Reference");
  private static final Supplier<Type> WEAK_HASH_MAP_TYPE =
      Suppliers.typeFromString("java.util.WeakHashMap");
  private static final Supplier<Type> IDENTITY_HASH_MAP_TYPE =
      Suppliers.typeFromString("java.util.IdentityHashMap");

  /**
   * Value-based classes defined in standard libraries (e.g. {@code java.util}, {@code java.time},
   * {@code java.lang}) that are migrating to value objects in Valhalla / JEP 401. This explicit set
   * is checked in addition to the {@code @jdk.internal.ValueBased} annotation for environments or
   * older JDK versions where the annotation is not present or visible on the classpath.
   */
  private static final ImmutableSet<String> VALUE_CLASSES_BESIDES_PRIMITIVE_WRAPPERS =
      ImmutableSet.of(
          // keep-sorted start
          "java.lang.ProcessHandle",
          "java.lang.Runtime.Version",
          "java.time.Duration",
          "java.time.Instant",
          "java.time.LocalDate",
          "java.time.LocalDateTime",
          "java.time.LocalTime",
          "java.time.MonthDay",
          "java.time.OffsetDateTime",
          "java.time.OffsetTime",
          "java.time.Period",
          "java.time.Year",
          "java.time.YearMonth",
          "java.time.ZoneId",
          "java.time.ZoneOffset",
          "java.time.ZonedDateTime",
          "java.util.Optional",
          "java.util.OptionalDouble",
          "java.util.OptionalInt",
          "java.util.OptionalLong"
          // keep-sorted end
          );

  private static final Matcher<ExpressionTree> SYSTEM_IDENTITY_HASH_CODE =
      staticMethod().onClass("java.lang.System").named("identityHashCode");

  private static final Matcher<ExpressionTree> OBJECT_WAIT_NOTIFY =
      instanceMethod().onDescendantOf("java.lang.Object").namedAnyOf("wait", "notify", "notifyAll");

  private static final Matcher<ExpressionTree> CACHE_OR_MAP_BUILDER =
      anyOf(
          instanceMethod()
              .onDescendantOfAny(
                  "com.google.common.cache.CacheBuilder",
                  "com.github.benmanes.caffeine.cache.Caffeine")
              .namedAnyOf("build", "buildAsync"),
          instanceMethod().onDescendantOf("com.google.common.collect.MapMaker").named("makeMap"));

  // anyClass() is safe here because these matchers are only checked when walking the receiver
  // chain of an already matched CACHE_OR_MAP_BUILDER call.
  private static final Matcher<ExpressionTree> WEAK_KEYS =
      instanceMethod().anyClass().named("weakKeys");

  private static final Matcher<ExpressionTree> WEAK_OR_SOFT_VALUES =
      instanceMethod().anyClass().namedAnyOf("weakValues", "softValues");

  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree.getExpression());
    if (isValueClass(type, state)) {
      return buildDescription(tree)
          .setMessage(
              message(
                  "synchronizing on a value-based class is unsafe and will fail in a future"
                      + " version of Java."))
          .build();
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (checkSupertypeIdentity(
        type, REFERENCE_TYPE.get(state), /* alsoCheckSecondTypeArg= */ false, state)) {
      return buildDescription(tree)
          .setMessage(
              message("using a value-based class as a referent type in Reference is unsafe."))
          .build();
    }
    if (checkSupertypeIdentity(
        type, WEAK_HASH_MAP_TYPE.get(state), /* alsoCheckSecondTypeArg= */ false, state)) {
      return buildDescription(tree)
          .setMessage(message("using a value-based class key in WeakHashMap is unsafe."))
          .build();
    }
    if (checkSupertypeIdentity(
        type, IDENTITY_HASH_MAP_TYPE.get(state), /* alsoCheckSecondTypeArg= */ true, state)) {
      return buildDescription(tree)
          .setMessage(
              message("using a value-based class as a key or value in IdentityHashMap is unsafe."))
          .build();
    }
    return Description.NO_MATCH;
  }

  private static boolean checkSupertypeIdentity(
      Type type, Type superType, boolean alsoCheckSecondTypeArg, VisitorState state) {
    if (superType == null || !ASTHelpers.isSubtype(type, superType, state)) {
      return false;
    }
    List<Type> typeArgs = state.getTypes().asSuper(type, superType.tsym).getTypeArguments();
    if (typeArgs.isEmpty()) {
      return false;
    }
    return isValueClass(typeArgs.get(0), state)
        || (alsoCheckSecondTypeArg && typeArgs.size() >= 2 && isValueClass(typeArgs.get(1), state));
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (SYSTEM_IDENTITY_HASH_CODE.matches(tree, state)) {
      if (isValueClass(ASTHelpers.getType(tree.getArguments().get(0)), state)) {
        return buildDescription(tree)
            .setMessage(
                message(
                    "calling System.identityHashCode(...) on a value-based class is unsafe; use"
                        + " Objects.hashCode(...) instead."))
            .build();
      }
      return Description.NO_MATCH;
    }

    if (OBJECT_WAIT_NOTIFY.matches(tree, state)) {
      Type receiverType = ASTHelpers.getReceiverType(tree);
      if (isValueClass(receiverType, state)) {
        return buildDescription(tree)
            .setMessage(
                message(
                    "calling %s() on a value-based class is unsafe and will fail in a future"
                        + " version of Java.",
                    ASTHelpers.getSymbol(tree).getSimpleName()))
            .build();
      }
      return Description.NO_MATCH;
    }

    if (CACHE_OR_MAP_BUILDER.matches(tree, state)) {
      Type returnType = ASTHelpers.getType(tree);
      if (returnType != null && returnType.getTypeArguments().size() >= 2) {
        Type keyType = returnType.getTypeArguments().get(0);
        Type valueType = returnType.getTypeArguments().get(1);
        if (hasMethodCallInChain(tree, WEAK_KEYS, state) && isValueClass(keyType, state)) {
          return buildDescription(tree)
              .setMessage(
                  message(
                      "using weakKeys() with a value-based class key in a Cache or MapMaker is"
                          + " unsafe."))
              .build();
        }
        if (hasMethodCallInChain(tree, WEAK_OR_SOFT_VALUES, state)
            && isValueClass(valueType, state)) {
          return buildDescription(tree)
              .setMessage(
                  message(
                      "using weakValues() or softValues() with a value-based class value in a"
                          + " Cache or MapMaker is unsafe."))
              .build();
        }
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean hasMethodCallInChain(
      ExpressionTree tree, Matcher<ExpressionTree> matcher, VisitorState state) {
    while (tree instanceof MethodInvocationTree methodInvocation) {
      if (matcher.matches(methodInvocation, state)) {
        return true;
      }
      tree = ASTHelpers.getReceiver(methodInvocation);
    }
    return false;
  }

  private static String message(String format, Object... args) {
    return String.format("Value-based classes do not have identity; " + format, args);
  }

  private static boolean isValueClass(Type type, VisitorState state) {
    if (type == null) {
      return false;
    }
    type = ASTHelpers.getUpperBound(type, state.getTypes());
    if (state.getTypes().unboxedType(type).isPrimitive()) {
      return true;
    }
    Symbol sym = type.tsym;
    return ASTHelpers.hasAnnotation(sym, "jdk.internal.ValueBased", state)
        || VALUE_CLASSES_BESIDES_PRIMITIVE_WRAPPERS.contains(sym.getQualifiedName().toString());
  }
}
