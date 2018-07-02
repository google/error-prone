/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.errorprone.util.Signatures.prettyType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(
    name = "PredicateIncompatibleType",
    category = JDK,
    summary =
        "Using ::equals or ::isInstance as an incompatible Predicate;"
            + " the predicate will always return false",
    severity = ERROR)
public class PredicateIncompatibleType extends BugChecker implements MemberReferenceTreeMatcher {

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {

    Type predicateType = predicateType(ASTHelpers.getType(tree), state);
    if (predicateType == null) {
      return NO_MATCH;
    }
    Type receiverType = ASTHelpers.getReceiverType(tree);

    if (tree.getName().contentEquals("equals")
        && !EqualsIncompatibleType.compatibilityOfTypes(receiverType, predicateType, state)
            .compatible()) {
      return buildMessage(receiverType, predicateType, tree);
    }

    if (tree.getName().contentEquals("isInstance")
        && ASTHelpers.isSameType(receiverType, state.getSymtab().classType, state)
        && !receiverType.getTypeArguments().isEmpty()) {
      Type argumentType = receiverType.getTypeArguments().get(0);
      Type upperBound = ASTHelpers.getUpperBound(predicateType, state.getTypes());
      if (!EqualsIncompatibleType.compatibilityOfTypes(upperBound, argumentType, state)
          .compatible()) {
        return buildMessage(upperBound, argumentType, tree);
      }
    }

    return NO_MATCH;
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

  private Description buildMessage(Type type1, Type type2, MemberReferenceTree tree) {
    return buildDescription(tree)
        .setMessage(
            String.format(
                "Predicate will always evaluate to false because types %s and %s are incompatible",
                prettyType(type1), prettyType(type2)))
        .build();
  }
}
