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

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils.TypeCompatibilityReport;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Types;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    severity = ERROR)
public class CollectionIncompatibleType extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {

  private enum FixType {
    NONE,
    CAST,
    PRINT_TYPES_AS_COMMENT,
    SUPPRESS_WARNINGS,
  }

  private final FixType fixType;
  private final TypeCompatibilityUtils typeCompatibilityUtils;

  public CollectionIncompatibleType(ErrorProneFlags flags) {
    this.fixType =
        flags.getEnum("CollectionIncompatibleType:FixType", FixType.class).orElse(FixType.NONE);
    this.typeCompatibilityUtils = TypeCompatibilityUtils.fromFlags(flags);
  }


  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return match(tree, state);
  }

  public Description match(ExpressionTree tree, VisitorState state) {
    MatchResult result = ContainmentMatchers.firstNonNullMatchResult(tree, state);
    if (result == null) {
      return NO_MATCH;
    }

    Types types = state.getTypes();
    TypeCompatibilityReport compatibilityReport =
        typeCompatibilityUtils.compatibilityOfTypes(
            result.targetType(), result.sourceType(), state);
    if (compatibilityReport.isCompatible()) {
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

    Description.Builder description =
        buildDescription(tree).setMessage(result.message(sourceType, targetType));

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

}
