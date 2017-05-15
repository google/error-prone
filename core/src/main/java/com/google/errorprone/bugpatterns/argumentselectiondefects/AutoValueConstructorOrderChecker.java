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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.NewClassTree;
import java.util.function.Function;

/**
 * Checker to make sure that constructors for AutoValue types are invoked with arguments in the
 * correct order.
 *
 * <p>Warning: this check relies on recovering parameter names from library class files. These names
 * are only included if you compile with debugging symbols (-g) or with -parameters. You also need
 * to tell the compiler to read these names from the classfiles and so must compile your project
 * with -parameters too.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@BugPattern(
  name = "AutoValueConstructorOrderChecker",
  summary = "Arguments to AutoValue constructor are in the wrong order",
  explanation =
      "AutoValue constructors are synthesized with their parameters in the same order as the "
          + "abstract accessor methods. Calls to the constructor need to match this ordering.",
  category = GUAVA,
  severity = ERROR
)
public class AutoValueConstructorOrderChecker extends BugChecker implements NewClassTreeMatcher {

  private ArgumentChangeFinder argumentChangeFinder =
      ArgumentChangeFinder.builder()
          .setDistanceFunction(buildDistanceFunction())
          .addHeuristic(allArgumentsMustMatch())
          .addHeuristic(new CreatesDuplicateCallHeuristic())
          .build();

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!Matchers.AUTOVALUE_CONSTRUCTOR.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    InvocationInfo invocationInfo =
        InvocationInfo.createFromNewClass(tree, ASTHelpers.getSymbol(tree), state);

    Changes changes = argumentChangeFinder.findChanges(invocationInfo);

    if (changes.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(invocationInfo.tree())
        .addFix(changes.buildPermuteArgumentsFix(invocationInfo))
        .build();
  }

  private static Function<ParameterPair, Double> buildDistanceFunction() {
    return new Function<ParameterPair, Double>() {
      @Override
      public Double apply(ParameterPair parameterPair) {
        Parameter formal = parameterPair.formal();
        Parameter actual = parameterPair.actual();
        if (formal.isUnknownName() || actual.isUnknownName()) {
          return formal.index() == actual.index() ? 0.0 : 1.0;
        } else {
          return formal.name().equals(actual.name()) ? 0.0 : 1.0;
        }
      }
    };
  }

  private static Heuristic allArgumentsMustMatch() {
    return (changes, node, sym, state) -> changes.assignmentCost().stream().allMatch(c -> c < 1.0);
  }
}
