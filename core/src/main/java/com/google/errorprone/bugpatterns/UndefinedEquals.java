/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Flags types which do not have well-defined equals behavior.
 *
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(
    altNames = {"CollectionUndefinedEquality", "ArrayAsKeyOfSetOrMap"},
    summary = "This type is not guaranteed to implement a useful equals() method.",
    severity = WARNING)
public final class UndefinedEquals extends AbstractEqualsHashCodeUsageChecker {

  private static final String ARRAY_MESSAGE =
      """
      Arrays do not override equals() or hashCode, so comparisons will be done on reference \
      equality only. If neither deduplication nor lookup are needed, consider using a List \
      instead. Otherwise, use IdentityHashMap/Set, a Map from a library that handles \
      object arrays, or an Iterable/List of pairs.\
      """;

  private static final Matcher<ExpressionTree> IS_EQUAL_TO =
      instanceMethod().onDescendantOf("com.google.common.truth.Subject").named("isEqualTo");

  @Override
  protected Optional<String> problem(Type type, Usage usage, VisitorState state) {
    if (type.hasTag(TypeTag.ARRAY)) {
      // Direct comparisons of arrays are handled by ArrayEquals, and Truth and JUnit assertions
      // have special handling for arrays.
      return usage == Usage.HASH_KEY ? Optional.of(ARRAY_MESSAGE) : Optional.empty();
    }
    for (TypesWithUndefinedEquality badType : TypesWithUndefinedEquality.values()) {
      if (badType.matchesType(type, state)) {
        return Optional.of(badType.shortName() + " does not have well-defined equality semantics");
      }
    }
    return Optional.empty();
  }

  @Override
  protected Optional<SuggestedFix> equalityFix(
      MethodInvocationTree tree, Tree receiver, Tree argument, VisitorState state) {
    // Generate fix for certain Truth `isEqualTo` calls
    if (IS_EQUAL_TO.matches(tree, state)) {
      String methodText =
          state.getSourceForNode(tree.getMethodSelect()); // e.g. "assertThat(foo).isEqualTo"
      String assertThatWithArg = methodText.substring(0, methodText.lastIndexOf('.'));

      // If both the argument and receiver are subtypes of the given type, rewrites the isEqualTo
      // method invocation to use the replacement comparison method instead.
      BiFunction<Type, String, Optional<SuggestedFix>> generateTruthFix =
          (type, replacementMethod) -> {
            if (type != null
                && isSubtype(getType(argument), type, state)
                && isSubtype(getType(receiver), type, state)) {
              return Optional.of(
                  SuggestedFix.replace(
                      tree,
                      String.format(
                          "%s.%s(%s)",
                          assertThatWithArg, replacementMethod, state.getSourceForNode(receiver))));
            }
            return Optional.empty();
          };

      // If both are subtypes of Iterable, rewrite
      Type iterableType = state.getSymtab().iterableType;
      Type multimapType = MULTIMAP.get(state);
      Optional<SuggestedFix> fix =
          generateTruthFix
              .apply(iterableType, "containsExactlyElementsIn")
              .or(() -> generateTruthFix.apply(multimapType, "containsExactlyEntriesIn"));
      if (fix.isPresent()) {
        return fix;
      }
    }

    // Generate fix for CharSequence
    Type charSequenceType = CHAR_SEQUENCE.get(state);
    BiFunction<Tree, Tree, Optional<SuggestedFix>> generateCharSequenceFix =
        (maybeCharSequence, maybeString) -> {
          if (charSequenceType != null
              && isSameType(getType(maybeCharSequence), charSequenceType, state)
              && isSameType(getType(maybeString), state.getSymtab().stringType, state)) {
            return Optional.of(SuggestedFix.postfixWith(maybeCharSequence, ".toString()"));
          }
          return Optional.empty();
        };
    return generateCharSequenceFix
        .apply(receiver, argument)
        .or(() -> generateCharSequenceFix.apply(argument, receiver));
  }

  private static final Supplier<Type> MULTIMAP =
      typeFromString("com.google.common.collect.Multimap");

  private static final Supplier<Type> CHAR_SEQUENCE = typeFromString("java.lang.CharSequence");
}
