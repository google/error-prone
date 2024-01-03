/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.OPTIONAL;
import static com.google.errorprone.bugpatterns.checkreturnvalue.Rules.returnsEnclosingType;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isAbstract;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.Rules.ErrorProneMethodRule;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;

/** Rules for methods on Dagger {@code Component.Builder} and {@code Subcomponent.Builder} types. */
public final class DaggerRules {

  /**
   * Returns a rule that handles {@code @dagger.Component.Builder} types, making their fluent setter
   * methods' results ignorable.
   */
  public static ResultUseRule<VisitorState, Symbol> componentBuilders() {
    return new DaggerRule("dagger.Component.Builder");
  }

  /**
   * Returns a rule that handles {@code @dagger.Subcomponent.Builder} types, making their fluent
   * setter methods' results ignorable.
   */
  public static ResultUseRule<VisitorState, Symbol> subcomponentBuilders() {
    return new DaggerRule("dagger.Subcomponent.Builder");
  }

  /** Rules for methods on Dagger components and subcomponents. */
  private static final class DaggerRule extends ErrorProneMethodRule {

    private final String annotationName;

    DaggerRule(String annotationName) {
      this.annotationName = checkNotNull(annotationName);
    }

    @Override
    public String id() {
      return "@" + annotationName;
    }

    @Override
    public Optional<ResultUsePolicy> evaluateMethod(MethodSymbol method, VisitorState state) {
      if (method.getParameters().size() == 1
          && isAbstract(method)
          && hasAnnotation(enclosingClass(method), annotationName, state)
          && returnsEnclosingType(method, state)) {
        return Optional.of(OPTIONAL);
      }
      return Optional.empty();
    }
  }

  private DaggerRules() {}
}
