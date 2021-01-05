/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.lang.model.element.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "DoNotClaimAnnotations",
    summary =
        "Don't 'claim' annotations in annotation processors; Processor#process should"
            + " unconditionally return `false`",
    severity = WARNING)
public class DoNotClaimAnnotations extends BugChecker implements MethodTreeMatcher {

  private static final Supplier<Name> PROCESS_NAME = memoize(s -> s.getName("process"));

  private static final Supplier<ImmutableList<Type>> PARAMETER_TYPES =
      memoize(
          s ->
              Stream.of("java.util.Set", "javax.annotation.processing.RoundEnvironment")
                  .map(s::getTypeFromString)
                  .filter(x -> x != null)
                  .collect(toImmutableList()));

  private static final Supplier<Symbol> PROCESSOR_SYMBOL =
      memoize(s -> s.getSymbolFromString("javax.annotation.processing.Processor"));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!tree.getName().equals(PROCESS_NAME.get(state))) {
      return NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (!ASTHelpers.isSameType(sym.getReturnType(), state.getSymtab().booleanType, state)) {
      return NO_MATCH;
    }
    if (sym.getParameters().size() != 2) {
      return NO_MATCH;
    }
    if (!Streams.zip(
            sym.getParameters().stream(),
            PARAMETER_TYPES.get(state).stream(),
            (p, t) -> ASTHelpers.isSameType(p.asType(), t, state))
        .allMatch(x -> x)) {
      return NO_MATCH;
    }
    if (!sym.owner.enclClass().isSubClass(PROCESSOR_SYMBOL.get(state), state.getTypes())) {
      return NO_MATCH;
    }
    List<ReturnTree> returns = new ArrayList<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void aVoid) {
        return null;
      }

      @Override
      public Void visitClass(ClassTree node, Void aVoid) {
        return null;
      }

      @Override
      public Void visitReturn(ReturnTree node, Void unused) {
        if (!Objects.equals(ASTHelpers.constValue(node.getExpression(), Boolean.class), false)) {
          returns.add(node);
        }
        return super.visitReturn(node, null);
      }
    }.scan(tree.getBody(), null);
    if (returns.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (ReturnTree returnTree : returns) {
      if (Objects.equals(ASTHelpers.constValue(returnTree.getExpression(), Boolean.class), true)) {
        fix.replace(returnTree.getExpression(), "false");
      }
    }
    return describeMatch(returns.get(0), fix.build());
  }
}
