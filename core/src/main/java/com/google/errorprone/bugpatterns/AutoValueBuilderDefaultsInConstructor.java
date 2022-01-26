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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.lang.String.join;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;
import javax.lang.model.element.Modifier;

/** See summary for details. */
@BugPattern(
    summary =
        "Defaults for AutoValue Builders should be set in the factory method returning Builder"
            + " instances, not the constructor",
    severity = ERROR)
public final class AutoValueBuilderDefaultsInConstructor extends BugChecker
    implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (!symbol.isConstructor()) {
      return NO_MATCH;
    }
    if (!hasAnnotation(symbol.owner, "com.google.auto.value.AutoValue.Builder", state)) {
      return NO_MATCH;
    }
    ImmutableList<String> invocations =
        extractInvocations(symbol, tree.getBody().getStatements(), state);
    if (invocations.isEmpty()) {
      return NO_MATCH;
    }

    return describeMatch(tree, appendDefaultsToConstructors(tree, symbol, invocations, state));
  }

  private static SuggestedFix appendDefaultsToConstructors(
      MethodTree tree, MethodSymbol symbol, ImmutableList<String> invocations, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder().delete(tree);
    String defaultSetters = "." + join(".", invocations);

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitNewClass(NewClassTree tree, Void unused) {
        if (isSubtype(getType(tree), symbol.owner.type, state)) {
          fix.postfixWith(tree, defaultSetters);
        }
        return super.visitNewClass(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  private static ImmutableList<String> extractInvocations(
      MethodSymbol symbol, List<? extends StatementTree> statements, VisitorState state) {
    return statements.stream()
        .filter(t -> t instanceof ExpressionStatementTree)
        .map(t -> (ExpressionStatementTree) t)
        .filter(t -> t.getExpression() instanceof MethodInvocationTree)
        .map(t -> (MethodInvocationTree) t.getExpression())
        .filter(
            t -> {
              Symbol calledSymbol = getSymbol(t.getMethodSelect());
              return calledSymbol.owner.equals(symbol.owner)
                  && calledSymbol.getModifiers().contains(Modifier.ABSTRACT);
            })
        .map(t -> state.getSourceForNode(t).replaceFirst("^this\\.", ""))
        .collect(toImmutableList());
  }
}
