/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.addSuppressWarnings;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Types;
import java.util.Arrays;
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
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class CollectionIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  public enum FixType {
    NONE,
    CAST,
    PRINT_TYPES_AS_COMMENT,
    SUPPRESS_WARNINGS,
  }

  private final FixType fixType;

  /** Creates a new {@link CollectionIncompatibleType} checker that provides no fix. */
  public CollectionIncompatibleType() {
    this(FixType.NONE);
  }

  /** Creates a new {@link CollectionIncompatibleType} checker with the given {@code fixType}. */
  public CollectionIncompatibleType(FixType fixType) {
    this.fixType = fixType;
  }

  // The "normal" case of extracting the type of a method argument
  private static final Iterable<MethodArgMatcher> DIRECT_MATCHERS =
      Arrays.asList(
          // "Normal" cases, e.g. Collection#remove(Object)
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
  private static final Iterable<TypeArgOfMethodArgMatcher> TYPE_ARG_MATCHERS =
      Arrays.asList(
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

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MatchResult directResult = firstNonNullMatchResult(DIRECT_MATCHERS, tree, state);
    MatchResult typeArgResult = null;
    if (directResult == null) {
      typeArgResult = firstNonNullMatchResult(TYPE_ARG_MATCHERS, tree, state);
    }
    if (directResult == null && typeArgResult == null) {
      return Description.NO_MATCH;
    }
    Verify.verify(directResult == null ^ typeArgResult == null);
    MatchResult result = MoreObjects.firstNonNull(directResult, typeArgResult);

    Types types = state.getTypes();
    if (types.isCastable(
        result.sourceType(), types.erasure(ASTHelpers.getUpperBound(result.targetType(), types)))) {
      return Description.NO_MATCH;
    }

    // For error message, use simple names instead of fully qualified names unless they are
    // identical.
    String sourceTreeType = Signatures.prettyType(ASTHelpers.getType(result.sourceTree()));
    String sourceType = Signatures.prettyType(result.sourceType());
    String targetType = Signatures.prettyType(result.targetType());
    if (sourceType.equals(targetType)) {
      sourceType = result.sourceType().toString();
      targetType = result.targetType().toString();
    }

    Description.Builder description = buildDescription(tree);
    if (typeArgResult != null) {
      description.setMessage(
          String.format(
              "Argument '%s' should not be passed to this method; its type %s has a type argument "
                  + "%s that is not compatible with its collection's type argument %s",
              result.sourceTree(), sourceTreeType, sourceType, targetType));
    } else {
      description.setMessage(
          String.format(
              "Argument '%s' should not be passed to this method; its type %s is not compatible "
                  + "with its collection's type argument %s",
              result.sourceTree(), sourceType, targetType));
    }

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
        Fix fix;
        if (typeArgResult != null) {
          TypeArgOfMethodArgMatcher matcher = (TypeArgOfMethodArgMatcher) typeArgResult.matcher();
          String fullyQualifiedType = matcher.getMethodArgTypeName();
          String simpleType = Iterables.getLast(Splitter.on('.').split(fullyQualifiedType));
          fix =
              SuggestedFix.builder()
                  .prefixWith(result.sourceTree(), String.format("(%s<?>) ", simpleType))
                  .addImport(fullyQualifiedType)
                  .build();
        } else {
          fix = SuggestedFix.prefixWith(result.sourceTree(), "(Object) ");
        }
        description.addFix(fix);
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
      MethodInvocationTree tree,
      VisitorState state) {
    for (AbstractCollectionIncompatibleTypeMatcher matcher : matchers) {
      MatchResult result = matcher.matches(tree, state);
      if (result != null) {
        return result;
      }
    }

    return null;
  }
}
