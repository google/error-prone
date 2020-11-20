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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/**
 * Checks for {@code equals} implementations comparing non-corresponding fields.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "EqualsWrongThing",
    summary =
        "Comparing different pairs of fields/getters in an equals implementation is probably "
            + "a mistake.",
    severity = ERROR)
public final class EqualsWrongThing extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodInvocationTree> COMPARISON_METHOD =
      anyOf(staticMethod().onClass("java.util.Arrays").named("equals"), staticEqualsInvocation());

  private static final EnumSet<ElementKind> FIELD_TYPES =
      EnumSet.of(ElementKind.FIELD, ElementKind.METHOD);

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!equalsMethodDeclaration().matches(tree, state)) {
      return NO_MATCH;
    }

    ClassSymbol classSymbol = getSymbol(tree).enclClass();
    Set<ComparisonSite> suspiciousComparisons = new HashSet<>();

    new TreeScanner<Void, Void>() {
      @Override
      public Void visitBinary(BinaryTree node, Void unused) {
        if (node.getKind() == Kind.EQUAL_TO || node.getKind() == Kind.NOT_EQUAL_TO) {
          getDubiousComparison(classSymbol, node, node.getLeftOperand(), node.getRightOperand())
              .ifPresent(suspiciousComparisons::add);
        }

        return super.visitBinary(node, null);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        if (COMPARISON_METHOD.matches(node, state)) {
          getDubiousComparison(
                  classSymbol, node, node.getArguments().get(0), node.getArguments().get(1))
              .ifPresent(suspiciousComparisons::add);
        }
        if (instanceEqualsInvocation().matches(node, state)) {
          ExpressionTree receiver = getReceiver(node);
          if (receiver != null) {
            // Special-case super, for odd cases like `super.equals(this)`.
            if (!(receiver instanceof IdentifierTree
                && ((IdentifierTree) receiver).getName().contentEquals("super"))) {
              getDubiousComparison(classSymbol, node, receiver, node.getArguments().get(0))
                  .ifPresent(suspiciousComparisons::add);
            }
          }
        }
        return super.visitMethodInvocation(node, null);
      }
    }.scan(tree, null);

    // Fast path return.
    if (suspiciousComparisons.isEmpty()) {
      return NO_MATCH;
    }

    // Special case where comparisons are made of (a, b) and (b, a) to imply that order doesn't
    // matter.
    ImmutableSet<ComparisonPair> suspiciousPairs =
        suspiciousComparisons.stream().map(ComparisonSite::pair).collect(toImmutableSet());
    suspiciousComparisons.stream()
        .filter(p -> !suspiciousPairs.contains(p.pair().reversed()))
        .map(
            c ->
                buildDescription(c.tree())
                    .setMessage(
                        String.format(
                            "Suspicious comparison between `%s` and `%s`",
                            c.pair().lhs(), c.pair().rhs()))
                    .build())
        .forEach(state::reportMatch);
    return NO_MATCH;
  }

  private static Optional<ComparisonSite> getDubiousComparison(
      ClassSymbol encl, Tree tree, ExpressionTree lhs, ExpressionTree rhs) {
    Symbol lhsSymbol = getSymbol(lhs);
    Symbol rhsSymbol = getSymbol(rhs);
    if (lhsSymbol == null || rhsSymbol == null || lhsSymbol.equals(rhsSymbol)) {
      return Optional.empty();
    }
    if (lhsSymbol.isStatic() || rhsSymbol.isStatic()) {
      return Optional.empty();
    }
    if (!encl.equals(lhsSymbol.enclClass()) || !encl.equals(rhsSymbol.enclClass())) {
      return Optional.empty();
    }
    if (!FIELD_TYPES.contains(lhsSymbol.getKind()) || !FIELD_TYPES.contains(rhsSymbol.getKind())) {
      return Optional.empty();
    }
    if (getKind(lhs) != getKind(rhs)) {
      return Optional.empty();
    }
    return Optional.of(ComparisonSite.of(tree, lhsSymbol, rhsSymbol));
  }

  private static Kind getKind(Tree tree) {
    Kind kind = tree.getKind();
    // Treat identifiers as being similar to member selects for our purposes, as in a == that.a.
    return kind == Kind.IDENTIFIER ? Kind.MEMBER_SELECT : kind;
  }

  @AutoValue
  abstract static class ComparisonSite {
    abstract Tree tree();

    abstract ComparisonPair pair();

    private static ComparisonSite of(Tree tree, Symbol lhs, Symbol rhs) {
      return new AutoValue_EqualsWrongThing_ComparisonSite(tree, ComparisonPair.of(lhs, rhs));
    }
  }

  @AutoValue
  abstract static class ComparisonPair {
    abstract Symbol lhs();

    abstract Symbol rhs();

    final ComparisonPair reversed() {
      return of(rhs(), lhs());
    }

    private static ComparisonPair of(Symbol lhs, Symbol rhs) {
      return new AutoValue_EqualsWrongThing_ComparisonPair(lhs, rhs);
    }
  }
}
