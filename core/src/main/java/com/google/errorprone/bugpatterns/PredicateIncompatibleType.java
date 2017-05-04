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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.Signatures.prettyType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "PredicateIncompatibleType",
  category = JDK,
  summary = "Using ::equals as an incompatible Predicate; the predicate will always return false",
  severity = ERROR
)
public class PredicateIncompatibleType extends BugChecker implements MemberReferenceTreeMatcher {

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!tree.getName().contentEquals("equals")) {
      return NO_MATCH;
    }
    Type predicateType = predicateType(ASTHelpers.getType(tree), state);
    Type receiverType = getReceiverType(tree);
    if (!EqualsIncompatibleType.incompatibleTypes(receiverType, predicateType, state)) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(
            String.format(
                "Using %s::equals as Predicate<%s>; the predicate will always return false",
                prettyType(receiverType), prettyType(predicateType)))
        .build();
  }

  private static Type predicateType(Type type, VisitorState state) {
    Symbol predicate = state.getSymbolFromString(java.util.function.Predicate.class.getName());
    if (predicate == null) {
      return null;
    }
    Type asPredicate = state.getTypes().asSuper(type, predicate);
    if (asPredicate == null) {
      return null;
    }
    return getOnlyElement(asPredicate.getTypeArguments(), null);
  }
}
