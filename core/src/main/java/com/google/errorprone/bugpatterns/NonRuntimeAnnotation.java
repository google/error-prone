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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Attribute.RetentionPolicy;
import com.sun.tools.javac.code.Type;
import javax.inject.Inject;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(
    summary = "Calling getAnnotation on an annotation that is not retained at runtime",
    severity = ERROR)
public class NonRuntimeAnnotation extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> LEGACY_MATCHER =
      instanceMethod()
          .onExactClass("java.lang.Class")
          .named("getAnnotation")
          .withParameters("java.lang.Class");

  private static final Matcher<ExpressionTree> ADDITIONAL_APIS_MATCHER =
      instanceMethod()
          .onDescendantOf("java.lang.reflect.AnnotatedElement")
          .namedAnyOf(
              "getAnnotation",
              "isAnnotationPresent",
              "getAnnotationsByType",
              "getDeclaredAnnotation",
              "getDeclaredAnnotationsByType")
          .withParameters("java.lang.Class");

  private final Matcher<ExpressionTree> matcher;

  @Inject
  NonRuntimeAnnotation(ErrorProneFlags flags) {
    this.matcher =
        flags.getBoolean("NonRuntimeAnnotation:AdditionalApis").orElse(true)
            ? ADDITIONAL_APIS_MATCHER
            : LEGACY_MATCHER;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!matcher.matches(tree, state)) {
      return NO_MATCH;
    }
    Type classType = getType(getOnlyElement(tree.getArguments()));
    if (classType == null || classType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }
    Type type = getUpperBound(getOnlyElement(classType.getTypeArguments()), state.getTypes());
    if (isSameType(type, state.getSymtab().annotationType, state)) {
      return NO_MATCH;
    }
    RetentionPolicy retention = state.getTypes().getRetention(type.asElement());
    switch (retention) {
      case RUNTIME -> {}
      case SOURCE, CLASS -> {
        return buildDescription(tree)
            .setMessage(
                "Calling %s on an annotation that is not retained at runtime; %s has %s retention",
                getSymbol(tree).getSimpleName(), type.asElement().getSimpleName(), retention)
            .build();
      }
    }
    return NO_MATCH;
  }
}
