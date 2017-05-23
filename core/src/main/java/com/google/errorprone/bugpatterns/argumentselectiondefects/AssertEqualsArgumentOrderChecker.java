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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import java.util.function.Function;
import javax.lang.model.element.ElementKind;

/**
 * Checker to make sure that assertEquals-like methods are called with the arguments expected and
 * actual the right way round.
 *
 * <p>Warning: this check relies on recovering parameter names from library class files. These names
 * are only included if you compile with debugging symbols (-g) or with -parameters. You also need
 * to tell the compiler to read these names from the classfiles and so must compile your project
 * with -parameters too.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@BugPattern(
  name = "AssertEqualsArgumentOrderChecker",
  summary = "Arguments are swapped in assertEquals-like call",
  explanation =
      "JUnit's assertEquals (and similar) are defined to take the expected value first and the "
          + "actual value second. Getting these the wrong way round will cause a confusing error "
          + "message if the assertion fails.",
  category = JUNIT,
  severity = WARNING
)
public class AssertEqualsArgumentOrderChecker extends BugChecker
    implements MethodInvocationTreeMatcher {

  private final ArgumentChangeFinder argumentchangeFinder =
      ArgumentChangeFinder.builder()
          .setDistanceFunction(buildDistanceFunction())
          .addHeuristic(changeMustBeBetterThanOriginal())
          .addHeuristic(new CreatesDuplicateCallHeuristic())
          .addHeuristic(new NameInCommentHeuristic())
          .build();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!Matchers.ASSERT_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }

    InvocationInfo invocationInfo = InvocationInfo.createFromMethodInvocation(tree, symbol, state);

    Changes changes = argumentchangeFinder.findChanges(invocationInfo);

    if (changes.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(invocationInfo.tree())
        .addFix(changes.buildPermuteArgumentsFix(invocationInfo))
        .addFix(changes.buildCommentArgumentsFix(invocationInfo))
        .build();
  }

  /**
   * This function looks explicitly for parameters named expected and actual. All other pairs with
   * parameters other than these are given a distance of 0 if they are in their original position
   * and Inf otherwise (i.e. they will not be considered for moving). For expected and actual, if
   * the actual parameter name starts with expected or actual respectively then we consider it a
   * perfect match otherwise we return a distance of 1.
   */
  private static Function<ParameterPair, Double> buildDistanceFunction() {
    return new Function<ParameterPair, Double>() {

      @Override
      public Double apply(ParameterPair parameterPair) {
        Parameter formal = parameterPair.formal();
        Parameter actual = parameterPair.actual();
        String formalName = formal.name();
        String actualName = actual.name();

        if (formalName.equals("expected")) {
          if (actual.constant() || isEnumIdentifier(actual)) {
            return 0.0;
          }
          if (actualName.startsWith("expected")) {
            return 0.0;
          }
          return 1.0;
        }

        if (formalName.equals("actual")) {
          if (actual.constant() || isEnumIdentifier(actual)) {
            return 1.0;
          }
          if (actualName.startsWith("actual")) {
            return 0.0;
          }
          return 1.0;
        }

        return formal.index() == actual.index() ? 0.0 : Double.POSITIVE_INFINITY;
      }
    };
  }

  /** Returns true if this parameter is an enum identifier */
  private static boolean isEnumIdentifier(Parameter parameter) {
    switch (parameter.kind()) {
      case IDENTIFIER:
      case MEMBER_SELECT:
        break;
      default:
        return false;
    }
    TypeSymbol typeSymbol = parameter.type().tsym;
    if (typeSymbol != null) {
      return typeSymbol.getKind() == ElementKind.ENUM;
    }
    return false;
  }

  private static Heuristic changeMustBeBetterThanOriginal() {
    return (changes, node, sym, state) ->
        changes.totalAssignmentCost() < changes.totalOriginalCost();
  }
}
