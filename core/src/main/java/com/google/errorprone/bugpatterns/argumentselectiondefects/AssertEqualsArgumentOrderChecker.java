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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArguments;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
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
      "assertEquals (and similar) are defined to take the expected value first and the actual "
          + "value second. Getting these the wrong way round will cause a confusing error "
          + "message if the assertion fails.",
  category = JDK,
  severity = WARNING
)
public class AssertEqualsArgumentOrderChecker extends ArgumentSelectionDefectChecker {

  private final ImmutableList<String> assertClassNames;

  public AssertEqualsArgumentOrderChecker() {
    this(
        ImmutableList.of("org.junit.Assert", "junit.framework.TestCase", "junit.framework.Assert"));
  }

  @VisibleForTesting
  AssertEqualsArgumentOrderChecker(ImmutableList<String> assertClassNames) {
    super(
        /*nameDistanceFunction=*/ buildDistanceFunction(),
        /*heuristics=*/ ImmutableList.of(
            changeMustBeBetterThanOriginal(), new CreatesDuplicateCallHeuristic()));
    this.assertClassNames = assertClassNames;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!assertMethodMatcher().matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return super.matchMethodInvocation(tree, state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return Description.NO_MATCH;
  }

  private Matcher<MethodInvocationTree> assertMethodMatcher() {
    return allOf(
        staticMethod().onClassAny(assertClassNames).withNameMatching(Pattern.compile("assert.*")),
        anyOf(twoParameterAssertMatcher(), threeParameterAssertMatcher()),
        not(argumentExtendsThrowableMatcher()),
        not(methodAnnotatedWithBeforeTemplateMatcher()));
  }

  // if any of the arguments are instances of throwable then abort - people like to use
  // 'expected' as the name of the exception they are expecting
  private static Matcher<MethodInvocationTree> argumentExtendsThrowableMatcher() {
    return hasArguments(MatchType.AT_LEAST_ONE, isSubtypeOf(Throwable.class));
  }

  // if the method is a refaster-before template then it might be explicitly matching bad behaviour
  private static Matcher<MethodInvocationTree> methodAnnotatedWithBeforeTemplateMatcher() {
    return enclosingMethod(
        hasAnnotation("com.google.errorprone.refaster.annotation.BeforeTemplate"));
  }

  private static Matcher<MethodInvocationTree> twoParameterAssertMatcher() {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree tree, VisitorState state) {
        List<VarSymbol> parameters = ASTHelpers.getSymbol(tree).getParameters();
        if (parameters.size() != 2) {
          return false;
        }
        return ASTHelpers.isSameType(parameters.get(0).asType(), parameters.get(1).asType(), state);
      }
    };
  }

  private static Matcher<MethodInvocationTree> threeParameterAssertMatcher() {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree tree, VisitorState state) {
        List<VarSymbol> parameters = ASTHelpers.getSymbol(tree).getParameters();
        if (parameters.size() != 3) {
          return false;
        }
        return ASTHelpers.isSameType(
                parameters.get(0).asType(), state.getTypeFromString("java.lang.String"), state)
            && ASTHelpers.isSameType(parameters.get(1).asType(), parameters.get(2).asType(), state);
      }
    };
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
          if (actual.isConstant() || isEnumType(actual.type())) {
            return 0.0;
          }
          if (actualName.startsWith("expected")) {
            return 0.0;
          }
          return 1.0;
        }

        if (formalName.equals("actual")) {
          if (actual.isConstant() || isEnumType(actual.type())) {
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

  private static boolean isEnumType(Type t) {
    TypeSymbol typeSymbol = t.tsym;
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
