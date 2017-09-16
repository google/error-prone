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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Equivalence;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "UnnecessarySetDefault",
  summary = "Unnecessary call to NullPointerTester#setDefault",
  severity = SUGGESTION
)
public class UnnecessarySetDefault extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> SET_DEFAULT =
      instanceMethod()
          .onExactClass("com.google.common.testing.NullPointerTester")
          .withSignature("<T>setDefault(java.lang.Class<T>,T)");

  @VisibleForTesting
  static final ImmutableMap<String, Matcher<ExpressionTree>> DEFAULTS =
      ImmutableMap.<String, Matcher<ExpressionTree>>builder()
          .put("java.lang.reflect.Type", sourceMatcher("Object.class"))
          .put("java.lang.reflect.GenericDeclaration", sourceMatcher("Object.class"))
          .put("java.lang.reflect.AnnotatedElement", sourceMatcher("Object.class"))
          .put(
              "com.google.common.collect.SortedMapDifference",
              sourceMatcher("Maps.difference(ImmutableSortedMap.of(), ImmutableSortedMap.of())"))
          .put(
              "com.google.common.collect.MapDifference",
              sourceMatcher("Maps.difference(ImmutableMap.of(), ImmutableMap.of())"))
          .put("com.google.common.collect.Range", factoryMatcher(Range.class, "all"))
          .put(
              "com.google.common.collect.ImmutableClassToInstanceMap",
              sourceMatcher("ImmutableClassToInstanceMap.builder().build()"))
          .put(
              "com.google.common.collect.ClassToInstanceMap",
              sourceMatcher("ImmutableClassToInstanceMap.builder().build()"))
          .put(
              "com.google.common.collect.RowSortedTable",
              sourceMatcher("Tables.unmodifiableRowSortedTable(TreeBasedTable.create())"))
          .put(
              "com.google.common.collect.ImmutableTable",
              factoryMatcher(ImmutableTable.class, "of"))
          .put("com.google.common.collect.Table", factoryMatcher(ImmutableTable.class, "of"))
          .put(
              "com.google.common.collect.ImmutableBiMap",
              factoryMatcher(ImmutableBiMap.class, "of"))
          .put("com.google.common.collect.BiMap", factoryMatcher(ImmutableBiMap.class, "of"))
          .put(
              "com.google.common.collect.ImmutableSortedMultiset",
              factoryMatcher(ImmutableSortedMultiset.class, "of"))
          .put(
              "com.google.common.collect.SortedMultiset",
              factoryMatcher(ImmutableSortedMultiset.class, "of"))
          .put(
              "com.google.common.collect.ImmutableMultiset",
              factoryMatcher(ImmutableMultiset.class, "of"))
          .put("com.google.common.collect.Multiset", factoryMatcher(ImmutableMultiset.class, "of"))
          .put(
              "com.google.common.collect.SortedSetMultimap",
              sourceMatcher("Multimaps.unmodifiableSortedSetMultimap(TreeMultimap.create())"))
          .put(
              "com.google.common.collect.ImmutableSetMultimap",
              factoryMatcher(ImmutableSetMultimap.class, "of"))
          .put(
              "com.google.common.collect.SetMultimap",
              factoryMatcher(ImmutableSetMultimap.class, "of"))
          .put(
              "com.google.common.collect.ImmutableListMultimap",
              factoryMatcher(ImmutableListMultimap.class, "of"))
          .put("com.google.common.collect.ListMultimap", factoryMatcher(ListMultimap.class, "of"))
          .put(
              "com.google.common.collect.ImmutableMultimap",
              factoryMatcher(ImmutableMultimap.class, "of"))
          .put("com.google.common.collect.Multimap", factoryMatcher(ImmutableMultimap.class, "of"))
          .put(
              "java.util.NavigableMap",
              sourceMatcher("Maps.unmodifiableNavigableMap(Maps.newTreeMap())"))
          .put(
              "com.google.common.collect.ImmutableSortedMap",
              factoryMatcher(ImmutableSortedMap.class, "of"))
          .put("java.util.SortedMap", factoryMatcher(ImmutableSortedMap.class, "of"))
          .put("com.google.common.collect.ImmutableMap", factoryMatcher(ImmutableMap.class, "of"))
          .put("java.util.Map", factoryMatcher(ImmutableMap.class, "of"))
          .put(
              "java.util.NavigableSet",
              sourceMatcher("Sets.unmodifiableNavigableSet(Sets.newTreeSet())"))
          .put(
              "com.google.common.collect.ImmutableSortedSet",
              factoryMatcher(ImmutableSortedSet.class, "of"))
          .put("java.util.SortedSet", factoryMatcher(ImmutableSortedSet.class, "of"))
          .put("com.google.common.collect.ImmutableSet", factoryMatcher(ImmutableSet.class, "of"))
          .put("java.util.Set", factoryMatcher(ImmutableSet.class, "of"))
          .put("com.google.common.collect.ImmutableList", factoryMatcher(ImmutableList.class, "of"))
          .put("java.util.List", factoryMatcher(ImmutableList.class, "of"))
          .put(
              "com.google.common.collect.ImmutableCollection",
              factoryMatcher(ImmutableList.class, "of"))
          .put("java.util.Collection", factoryMatcher(ImmutableList.class, "of"))
          .put("java.lang.Iterable", factoryMatcher(ImmutableSet.class, "of"))
          .put("java.util.ListIterator", sourceMatcher("ImmutableList.of().listIterator()"))
          .put(
              "com.google.common.collect.PeekingIterator",
              sourceMatcher("Iterators.peekingIterator(ImmutableSet.of().iterator())"))
          .put("java.util.Iterator", sourceMatcher("ImmutableSet.of().iterator()"))
          .put("com.google.common.io.CharSource", factoryMatcher(CharSource.class, "empty"))
          .put("com.google.common.io.ByteSource", factoryMatcher(ByteSource.class, "empty"))
          .put("java.io.File", sourceMatcher("new File(\"\")"))
          .put("java.nio.DoubleBuffer", sourceMatcher("DoubleBuffer.allocate(0)"))
          .put("java.nio.FloatBuffer", sourceMatcher("FloatBuffer.allocate(0)"))
          .put("java.nio.LongBuffer", sourceMatcher("LongBuffer.allocate(0)"))
          .put("java.nio.IntBuffer", sourceMatcher("IntBuffer.allocate(0)"))
          .put("java.nio.ShortBuffer", sourceMatcher("ShortBuffer.allocate(0)"))
          .put("java.nio.ByteBuffer", sourceMatcher("ByteBuffer.allocate(0)"))
          .put("java.nio.CharBuffer", sourceMatcher("CharBuffer.allocate(0)"))
          .put("java.nio.Buffer", sourceMatcher("ByteBuffer.allocate(0)"))
          .put("java.io.StringReader", sourceMatcher("new StringReader(\"\")"))
          .put("java.io.Reader", sourceMatcher("new StringReader(\"\")"))
          .put("java.lang.Readable", sourceMatcher("new StringReader(\"\")"))
          .put(
              "java.io.ByteArrayInputStream",
              sourceMatcher("new ByteArrayInputStream(new byte[0])"))
          .put("java.io.InputStream", sourceMatcher("new ByteArrayInputStream(new byte[0])"))
          .put(
              "com.google.common.base.Stopwatch",
              factoryMatcher(Stopwatch.class, "createUnstarted"))
          .put("com.google.common.base.Ticker", factoryMatcher(Ticker.class, "systemTicker"))
          .put("com.google.common.base.Equivalence", factoryMatcher(Equivalence.class, "equals"))
          .put("com.google.common.base.Predicate", factoryMatcher(Predicates.class, "alwaysTrue"))
          .put("com.google.common.base.Optional", factoryMatcher(Optional.class, "absent"))
          .put("com.google.common.base.Splitter", sourceMatcher("Splitter.on(',')"))
          .put("com.google.common.base.Joiner", sourceMatcher("Joiner.on(',')"))
          .put("com.google.common.base.CharMatcher", factoryMatcher(CharMatcher.class, "none"))
          .put("java.util.Locale", sourceMatcher("Locale.US"))
          .put("java.util.Currency", sourceMatcher("Currency.getInstance(Locale.US)"))
          .put("java.nio.charset.Charset", sourceMatcher("Charsets.UTF_8"))
          .put("java.util.concurrent.TimeUnit", sourceMatcher("TimeUnit.SECONDS"))
          .put("java.util.regex.Pattern", sourceMatcher("Pattern.compile(\"\")"))
          .put("java.lang.String", sourceMatcher("\"\""))
          .put("java.lang.CharSequence", sourceMatcher("\"\""))
          .put("java.math.BigDecimal", sourceMatcher("BigDecimal.ZERO"))
          .put("java.math.BigInteger", sourceMatcher("BigInteger.ZERO"))
          .put("com.google.common.primitives.UnsignedLong", sourceMatcher("UnsignedLong.ZERO"))
          .put(
              "com.google.common.primitives.UnsignedInteger", sourceMatcher("UnsignedInteger.ZERO"))
          .put("java.lang.Number", sourceMatcher("0"))
          .put("java.lang.Object", sourceMatcher("\"\""))
          .put("Optional.class", factoryMatcher(java.util.Optional.class, "empty"))
          .put("OptionalInt.class", factoryMatcher(OptionalInt.class, "empty"))
          .put("OptionalLong.class", factoryMatcher(OptionalLong.class, "empty"))
          .put("OptionalDouble.class", factoryMatcher(OptionalDouble.class, "empty"))
          .build();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!SET_DEFAULT.matches(tree, state)) {
      return NO_MATCH;
    }
    Type type = ASTHelpers.getType(tree.getArguments().get(0));
    if (type == null) {
      return NO_MATCH;
    }
    Type classType = state.getTypes().asSuper(type, state.getSymtab().classType.asElement());
    if (classType == null || classType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }
    String defaultTypeName =
        getOnlyElement(classType.getTypeArguments()).asElement().getQualifiedName().toString();
    if (!DEFAULTS.containsKey(defaultTypeName)) {
      return NO_MATCH;
    }
    Matcher<ExpressionTree> defaultType = DEFAULTS.get(defaultTypeName);
    if (!defaultType.matches(tree.getArguments().get(1), state)) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    Tree ancestor = state.getPath().getParentPath().getLeaf();
    if (ancestor instanceof ExpressionStatementTree) {
      description.addFix(SuggestedFix.delete(ancestor));
    } else if (receiver != null) {
      description.addFix(
          SuggestedFix.replace(state.getEndPosition(receiver), state.getEndPosition(tree), ""));
    }
    return description.build();
  }

  private static ParameterMatcher factoryMatcher(Class<?> clazz, String name) {
    return staticMethod().onClass(clazz.getCanonicalName()).named(name).withParameters();
  }

  static Matcher<ExpressionTree> sourceMatcher(String source) {
    return (tree, state) -> state.getSourceForNode(tree).equals(source);
  }
}
