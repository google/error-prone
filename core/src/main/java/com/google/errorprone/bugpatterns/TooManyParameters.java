/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isRecord;
import static com.google.errorprone.util.ASTHelpers.methodIsPublicAndNotAnOverride;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "A large number of parameters on public APIs should be avoided.",
    severity = WARNING)
public class TooManyParameters extends BugChecker implements MethodTreeMatcher {
  // In 'Detecting Argument Selection Defects' by Rice et. al., the authors argue that methods
  // should have 5 of fewer parameters (see section 7.1):
  // https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/46317.pdf
  // However, we have chosen a very conservative starting number, with hopes to decrease this in the
  // future.
  private static final int DEFAULT_LIMIT = 9;

  static final String TOO_MANY_PARAMETERS_FLAG_NAME = "TooManyParameters:ParameterLimit";

  private static final ImmutableSet<String> METHOD_ANNOTATIONS_TO_IGNORE =
      ImmutableSet.of(
          "java.lang.Deprecated",
          "java.lang.Override",
          // dependency injection annotations
          "jakarta.inject.Inject",
          "javax.inject.Inject",
          "com.google.inject.Inject",
          "com.google.inject.Provides",
          // JUnit @Test methods are never directly invoked (most of the time they don't even have
          // parameters, unless it's a parameterized test --- which still are not directly
          // invoked!); see b/303486200
          "org.junit.Test",
          // dagger provider / producers
          "dagger.Provides",
          "dagger.producers.Produces");

  private static final ImmutableSet<String> CLASS_ANNOTATIONS_TO_IGNORE =
      ImmutableSet.of("com.google.auto.factory.AutoFactory");

  private final int limit;

  @Inject
  TooManyParameters(ErrorProneFlags flags) {
    this.limit = flags.getInteger(TOO_MANY_PARAMETERS_FLAG_NAME).orElse(DEFAULT_LIMIT);
    checkArgument(limit > 0, "%s (%s) must be > 0", TOO_MANY_PARAMETERS_FLAG_NAME, limit);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    int paramCount = tree.getParameters().size();
    if (paramCount <= limit) {
      return NO_MATCH;
    }
    if (!shouldApplyApiChecks(tree, state)) {
      return NO_MATCH;
    }

    String name = getSymbol(tree).isConstructor() ? "constructor" : "method";
    String message =
        String.format(
            "Consider using a builder pattern instead of a %s with %s parameters. Data shows"
                + " that defining %s with > 5 parameters often leads to bugs. See also"
                + " Effective Java, Item 2.",
            name, paramCount, name);
    return buildDescription(tree).setMessage(message).build();
  }

  private static boolean shouldApplyApiChecks(MethodTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    if (symbol.owner instanceof ClassSymbol
        && CLASS_ANNOTATIONS_TO_IGNORE.stream()
            .anyMatch(a -> hasAnnotation(symbol.owner, a, state))) {
      return false;
    }
    if (isRecord(symbol)) {
      return false;
    }
    return METHOD_ANNOTATIONS_TO_IGNORE.stream().noneMatch(a -> hasAnnotation(tree, a, state))
        && methodIsPublicAndNotAnOverride(symbol, state);
  }
}
