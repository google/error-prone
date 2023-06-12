/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Consider inlining this constant", severity = WARNING)
public class InlineTrivialConstant extends BugChecker implements CompilationUnitTreeMatcher {

  @AutoValue
  abstract static class TrivialConstant {
    abstract VariableTree tree();

    abstract String replacement();

    static TrivialConstant create(VariableTree tree, String replacement) {
      return new AutoValue_InlineTrivialConstant_TrivialConstant(tree, replacement);
    }
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Map<VarSymbol, TrivialConstant> fields = new HashMap<>();
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VarSymbol sym = getSymbol(tree);
        isTrivialConstant(tree, sym, state)
            .ifPresent(r -> fields.put(sym, TrivialConstant.create(tree, r)));
        return super.visitVariable(tree, null);
      }
    }.scan(state.getPath(), null);
    ListMultimap<Symbol, Tree> uses = MultimapBuilder.hashKeys().arrayListValues().build();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        handle(tree);
        return super.visitMemberSelect(tree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        handle(tree);
        return super.visitIdentifier(tree, null);
      }

      private void handle(Tree tree) {
        Symbol sym = getSymbol(tree);
        if (sym == null) {
          return;
        }
        if (fields.containsKey(sym)) {
          uses.put(sym, tree);
        }
      }
    }.scan(state.getPath(), null);
    for (Map.Entry<VarSymbol, TrivialConstant> e : fields.entrySet()) {
      SuggestedFix.Builder fix = SuggestedFix.builder();
      TrivialConstant value = e.getValue();
      fix.delete(value.tree());
      uses.get(e.getKey()).forEach(x -> fix.replace(x, value.replacement()));
      state.reportMatch(describeMatch(value.tree(), fix.build()));
    }
    return NO_MATCH;
  }

  private static final ImmutableSet<String> EMPTY_STRING_VARIABLE_NAMES =
      ImmutableSet.of("EMPTY", "EMPTY_STR", "EMPTY_STRING");

  private static Optional<String> isTrivialConstant(
      VariableTree tree, VarSymbol sym, VisitorState state) {
    if (!(sym.getKind().equals(ElementKind.FIELD)
        && sym.getModifiers().contains(Modifier.PRIVATE)
        && sym.isStatic()
        && sym.getModifiers().contains(Modifier.FINAL))) {
      return Optional.empty();
    }
    if (!EMPTY_STRING_VARIABLE_NAMES.contains(tree.getName().toString())) {
      return Optional.empty();
    }
    if (!isSameType(sym.asType(), state.getSymtab().stringType, state)) {
      return Optional.empty();
    }
    ExpressionTree initializer = tree.getInitializer();
    if (initializer.getKind().equals(Tree.Kind.STRING_LITERAL)) {
      String value = (String) ((LiteralTree) initializer).getValue();
      if (Objects.equals(value, "")) {
        return Optional.of("\"\"");
      }
    }
    return Optional.empty();
  }
}
