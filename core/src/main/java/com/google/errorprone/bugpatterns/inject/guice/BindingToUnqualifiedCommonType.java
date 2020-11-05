/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.inject.guice;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_PROVIDES_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.classLiteral;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isPrimitiveOrBoxedPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;

/** @author glorioso@google.com (Nick Glorioso) */
@BugPattern(
    name = "BindingToUnqualifiedCommonType",
    summary =
        "This code declares a binding for a common value type without a Qualifier annotation.",
    severity = WARNING)
public class BindingToUnqualifiedCommonType extends BugChecker
    implements MethodTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<Tree> IS_SIMPLE_TYPE =
      anyOf(
          isPrimitiveOrBoxedPrimitiveType(),
          // java.time types
          isSameType(DayOfWeek.class),
          isSameType(Duration.class),
          isSameType(Instant.class),
          isSameType(LocalDate.class),
          isSameType(LocalDateTime.class),
          isSameType(LocalTime.class),
          isSameType(Month.class),
          isSameType(MonthDay.class),
          isSameType(OffsetDateTime.class),
          isSameType(OffsetTime.class),
          isSameType(Period.class),
          isSameType(Year.class),
          isSameType(YearMonth.class),
          isSameType(ZonedDateTime.class),
          // other common JDK types
          isSameType(String.class),
          isSameType(BigDecimal.class));

  private static final Matcher<MethodTree> PROVIDES_UNQUALIFIED_CONSTANT =
      allOf(
          annotations(AT_LEAST_ONE, isType(GUICE_PROVIDES_ANNOTATION)),
          not(
              annotations(
                  AT_LEAST_ONE,
                  Matchers.<AnnotationTree>anyOf(
                      symbolHasAnnotation(InjectMatchers.GUICE_BINDING_ANNOTATION),
                      symbolHasAnnotation(InjectMatchers.JAVAX_QUALIFIER_ANNOTATION)))),
          methodReturns(IS_SIMPLE_TYPE));

  private static final Matcher<MethodInvocationTree> BIND_TO_UNQUALIFIED_CONSTANT =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.inject.binder.LinkedBindingBuilder")
              .namedAnyOf("to", "toInstance", "toProvider", "toConstructor"),
          receiverOfInvocation(
              methodInvocation(
                  anyOf(
                      instanceMethod()
                          .onDescendantOf("com.google.inject.AbstractModule")
                          .withSignature("<T>bind(java.lang.Class<T>)"),
                      instanceMethod()
                          .onDescendantOf("com.google.inject.Binder")
                          .withSignature("<T>bind(java.lang.Class<T>)")),
                  MatchType.ALL,
                  classLiteral(IS_SIMPLE_TYPE))));

  @Override
  public Description matchMethod(MethodTree method, VisitorState state) {
    if (PROVIDES_UNQUALIFIED_CONSTANT.matches(method, state)
        && !ASTHelpers.isJUnitTestCode(state)) {
      return describeMatch(method);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocation, VisitorState state) {
    if (BIND_TO_UNQUALIFIED_CONSTANT.matches(methodInvocation, state)
        && !ASTHelpers.isJUnitTestCode(state)) {
      return describeMatch(methodInvocation);
    }
    return Description.NO_MATCH;
  }
}
