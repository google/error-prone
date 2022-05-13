/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.EXPECTED;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.OPTIONAL;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.sun.tools.javac.code.Flags.ABSTRACT;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.MethodRule;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;

/** Rules for {@code @AutoValue}, {@code @AutoValue.Builder}, and {@code @AutoBuilder} types. */
public final class AutoValueRules {
  private AutoValueRules() {}

  /** Returns a rule for {@code abstract} methods on {@code @AutoValue} types. */
  public static ResultUseRule autoValues() {
    return new ValueRule();
  }

  /** Returns a rule for {@code abstract} methods on {@code @AutoValue.Builder} types. */
  public static ResultUseRule autoValueBuilders() {
    return new BuilderRule("AutoValue.Builder");
  }

  /** Returns a rule for {@code abstract} methods on {@code @AutoBuilder} types. */
  public static ResultUseRule autoBuilders() {
    return new BuilderRule("AutoBuilder");
  }

  private static final class ValueRule extends AbstractAutoRule {
    ValueRule() {
      super("AutoValue");
    }

    @Override
    protected ResultUsePolicy autoMethodPolicy(
        MethodSymbol abstractMethod, ClassSymbol autoClass, VisitorState state) {
      return EXPECTED;
    }
  }

  private static final class BuilderRule extends AbstractAutoRule {
    BuilderRule(String annotation) {
      super(annotation);
    }

    @Override
    protected ResultUsePolicy autoMethodPolicy(
        MethodSymbol abstractMethod, ClassSymbol autoClass, VisitorState state) {
      return abstractMethod.getParameters().size() == 1
              && isSameType(abstractMethod.getReturnType(), autoClass.type, state)
          ? OPTIONAL
          : EXPECTED;
    }
  }

  private abstract static class AbstractAutoRule extends MethodRule {
    private static final String PACKAGE = "com.google.auto.value.";

    private final String annotation;

    AbstractAutoRule(String annotation) {
      this.annotation = annotation;
    }

    @Override
    public String id() {
      return '@' + annotation;
    }

    protected abstract ResultUsePolicy autoMethodPolicy(
        MethodSymbol abstractMethod, ClassSymbol autoClass, VisitorState state);

    private static boolean isAbstract(MethodSymbol method) {
      return (method.flags() & ABSTRACT) != 0;
    }

    @Override
    public Optional<ResultUsePolicy> evaluateMethod(MethodSymbol method, VisitorState state) {
      if (isAbstract(method)) {
        ClassSymbol enclosingClass = enclosingClass(method);
        if (hasAnnotation(enclosingClass, PACKAGE + annotation, state)) {
          return Optional.of(autoMethodPolicy(method, enclosingClass, state));
        }
      }
      return Optional.empty();
    }
  }
}
