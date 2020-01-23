/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.addSuppressWarnings;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOfAny;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.EqualsIncompatibleType;
import com.google.errorprone.bugpatterns.EqualsIncompatibleType.TypeCompatibilityReport;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Checker for calling Object-accepting methods with types that don't match the type arguments of
 * their container types. Currently this checker detects problems with the following methods on all
 * their subtypes and subinterfaces:
 *
 * <ul>
 *   <li>{@link Collection#contains}
 *   <li>{@link Collection#remove}
 *   <li>{@link List#indexOf}
 *   <li>{@link List#lastIndexOf}
 *   <li>{@link Map#get}
 *   <li>{@link Map#containsKey}
 *   <li>{@link Map#remove}
 *   <li>{@link Map#containsValue}
 * </ul>
 */
@BugPattern(
    name = "CollectionIncompatibleType",
    summary = "Incompatible type as argument to Object-accepting Java collections method",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class CollectionIncompatibleType extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {

  public enum FixType {
    NONE,
    CAST,
    PRINT_TYPES_AS_COMMENT,
    SUPPRESS_WARNINGS,
  }

  private final FixType fixType;

  /** Creates a new {@link CollectionIncompatibleType} checker that provides no fix. */
  public CollectionIncompatibleType(ErrorProneFlags flags) {
    this(FixType.SUPPRESS_WARNINGS, flags);
  }

  /** Creates a new {@link CollectionIncompatibleType} checker with the given {@code fixType}. */
  public CollectionIncompatibleType(FixType fixType, ErrorProneFlags flags) {
    this.fixType = fixType;
  }

  /**
   * The least-common ancestor of all of the types of {@link #DIRECT_MATCHERS} and {@link
   * #TYPE_ARG_MATCHERS}.
   */
  private static final Matcher<ExpressionTree> FIRST_ORDER_MATCHER =
      Matchers.anyMethod()
          .onClass(
              isDescendantOfAny(
                  ImmutableList.of(
                      "java.util.Collection",
                      "java.util.Dictionary",
                      "java.util.Map",
                      "java.util.Collections",
                      "com.google.common.collect.Sets")));

  // The "normal" case of extracting the type of a method argument
  private static final ImmutableList<MethodArgMatcher> DIRECT_MATCHERS =
      ImmutableList.of(
          // "Normal" cases, e.g. Collection#remove(Object)
          // Make sure to keep that the type or one of its supertype should be present in
          // FIRST_ORDER_MATCHER
          new MethodArgMatcher("java.util.Collection", "contains(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Collection", "remove(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Deque", "removeFirstOccurrence(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Deque", "removeLastOccurrence(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Dictionary", "get(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Dictionary", "remove(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.List", "indexOf(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.List", "lastIndexOf(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Map", "containsKey(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Map", "containsValue(java.lang.Object)", 1, 0),
          new MethodArgMatcher("java.util.Map", "get(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Map", "getOrDefault(java.lang.Object,V)", 0, 0),
          new MethodArgMatcher("java.util.Map", "remove(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Stack", "search(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Vector", "indexOf(java.lang.Object,int)", 0, 0),
          new MethodArgMatcher("java.util.Vector", "lastIndexOf(java.lang.Object,int)", 0, 0),
          new MethodArgMatcher("java.util.Vector", "removeElement(java.lang.Object)", 0, 0));

  // Cases where we need to extract the type argument from a method argument, e.g.
  // Collection#containsAll(Collection<?>)
  private static final ImmutableList<TypeArgOfMethodArgMatcher> TYPE_ARG_MATCHERS =
      ImmutableList.of(
          // Make sure to keep that the type or one of its supertype should be present in
          // FIRST_ORDER_MATCHER
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              "containsAll(java.util.Collection<?>)", // method signature
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0), // index of the method argument's type argument to extract
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              "removeAll(java.util.Collection<?>)", // method signature
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0), // index of the method argument's type argument to extract
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              "retainAll(java.util.Collection<?>)", // method signature
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0)); // index of the method argument's type argument to extract

  private static final ImmutableList<BinopMatcher> STATIC_MATCHERS =
      ImmutableList.of(
          new BinopMatcher("java.util.Collection", "java.util.Collections", "disjoint"),
          new BinopMatcher("java.util.Set", "com.google.common.collect.Sets", "difference"));

  private static final ImmutableList<AbstractCollectionIncompatibleTypeMatcher> ALL_MATCHERS =
      ImmutableList.<AbstractCollectionIncompatibleTypeMatcher>builder()
          .addAll(DIRECT_MATCHERS)
          .addAll(TYPE_ARG_MATCHERS)
          .addAll(STATIC_MATCHERS)
          .build();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return match(tree, state);
  }

  public Description match(ExpressionTree tree, VisitorState state) {
    if (!FIRST_ORDER_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }

    MatchResult result = firstNonNullMatchResult(ALL_MATCHERS, tree, state);
    if (result == null) {
      return NO_MATCH;
    }

    Types types = state.getTypes();
    TypeCompatibilityReport compatibilityReport =
        EqualsIncompatibleType.compatibilityOfTypes(
            result.targetType(), result.sourceType(), state);
    if (compatibilityReport.compatible()) {
      return NO_MATCH;
    }

    // For error message, use simple names instead of fully qualified names unless they are
    // identical.
    String sourceType = Signatures.prettyType(result.sourceType());
    String targetType = Signatures.prettyType(result.targetType());
    if (sourceType.equals(targetType)) {
      sourceType = result.sourceType().toString();
      targetType = result.targetType().toString();
    }

    Description.Builder description = buildDescription(tree);
    description.setMessage(result.message(sourceType, targetType));

    switch (fixType) {
      case PRINT_TYPES_AS_COMMENT:
        description.addFix(
            SuggestedFix.prefixWith(
                tree,
                String.format(
                    "/* expected: %s, actual: %s */",
                    ASTHelpers.getUpperBound(result.targetType(), types), result.sourceType())));
        break;
      case CAST:
        result.buildFix().ifPresent(description::addFix);
        break;
      case SUPPRESS_WARNINGS:
        SuggestedFix.Builder builder = SuggestedFix.builder();
        builder.prefixWith(
            result.sourceTree(),
            String.format("/* expected: %s, actual: %s */ ", targetType, sourceType));
        addSuppressWarnings(builder, state, "CollectionIncompatibleType");
        description.addFix(builder.build());
        break;
      case NONE:
        break;
    }

    return description.build();
  }

  @Nullable
  private static MatchResult firstNonNullMatchResult(
      Iterable<? extends AbstractCollectionIncompatibleTypeMatcher> matchers,
      ExpressionTree tree,
      VisitorState state) {
    for (AbstractCollectionIncompatibleTypeMatcher matcher : matchers) {
      MatchResult result = matcher.matches(tree, state);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private static class BinopMatcher extends AbstractCollectionIncompatibleTypeMatcher {

    private final Matcher<ExpressionTree> matcher;
    private final String collectionType;

    BinopMatcher(String collectionType, String className, String methodName) {
      this.collectionType = collectionType;
      matcher = Matchers.staticMethod().onClass(className).named(methodName);
    }

    @Override
    Matcher<ExpressionTree> methodMatcher() {
      return matcher;
    }

    @Nullable
    @Override
    Type extractSourceType(MethodInvocationTree tree, VisitorState state) {
      return extractTypeArgAsMemberOfSupertype(
          getType(tree.getArguments().get(0)),
          state.getSymbolFromString(collectionType),
          0,
          state.getTypes());
    }

    @Nullable
    @Override
    Type extractSourceType(MemberReferenceTree tree, VisitorState state) {
      Type descriptorType = state.getTypes().findDescriptorType(getType(tree));
      return extractTypeArgAsMemberOfSupertype(
          descriptorType.getParameterTypes().get(0),
          state.getSymbolFromString(collectionType),
          0,
          state.getTypes());
    }

    @Nullable
    @Override
    ExpressionTree extractSourceTree(MethodInvocationTree tree, VisitorState state) {
      return tree.getArguments().get(0);
    }

    @Nullable
    @Override
    ExpressionTree extractSourceTree(MemberReferenceTree tree, VisitorState state) {
      return tree;
    }

    @Nullable
    @Override
    Type extractTargetType(MethodInvocationTree tree, VisitorState state) {
      return extractTypeArgAsMemberOfSupertype(
          getType(tree.getArguments().get(1)),
          state.getSymbolFromString(collectionType),
          0,
          state.getTypes());
    }

    @Nullable
    @Override
    Type extractTargetType(MemberReferenceTree tree, VisitorState state) {
      Type descriptorType = state.getTypes().findDescriptorType(getType(tree));
      return extractTypeArgAsMemberOfSupertype(
          descriptorType.getParameterTypes().get(1),
          state.getSymbolFromString(collectionType),
          0,
          state.getTypes());
    }

    @Override
    protected String message(MatchResult result, String sourceType, String targetType) {
      return String.format(
          "Argument '%s' should not be passed to this method; its type %s is not compatible with"
              + " %s",
          result.sourceTree(), sourceType, targetType);
    }
  }
}
