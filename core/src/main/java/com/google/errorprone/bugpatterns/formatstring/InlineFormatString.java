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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ElementKind;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "InlineFormatString",
    summary =
        "Prefer to create format strings inline, instead of extracting them to a single-use"
            + " constant",
    severity = WARNING)
public class InlineFormatString extends BugChecker implements CompilationUnitTreeMatcher {

  private static final Matcher<ExpressionTree> PRECONDITIONS_CHECK =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Preconditions"),
              staticMethod().onClass("com.google.common.base.Verify")),
          InlineFormatString::secondParameterIsString);

  private static boolean secondParameterIsString(ExpressionTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (!(symbol instanceof MethodSymbol)) {
      return false;
    }
    MethodSymbol methodSymbol = (MethodSymbol) symbol;
    return methodSymbol.getParameters().size() >= 2
        && isSubtype(methodSymbol.getParameters().get(1).type, state.getSymtab().stringType, state);
  }

  @Nullable
  private static ExpressionTree formatString(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<ExpressionTree> args = FormatStringUtils.formatMethodArguments(tree, state);
    if (!args.isEmpty()) {
      return args.get(0);
    }
    if (PRECONDITIONS_CHECK.matches(tree, state)) {
      return tree.getArguments().get(1);
    }
    return formatMethodAnnotationArguments(tree, state);
  }

  @Nullable
  private static ExpressionTree formatMethodAnnotationArguments(
      MethodInvocationTree tree, VisitorState state) {
    MethodSymbol sym = getSymbol(tree);
    if (sym == null) {
      return null;
    }
    if (!ASTHelpers.hasAnnotation(sym, FormatMethod.class, state)) {
      return null;
    }
    return tree.getArguments().get(formatStringIndex(state, sym));
  }

  private static int formatStringIndex(VisitorState state, MethodSymbol sym) {
    int idx = 0;
    List<VarSymbol> parameters = sym.getParameters();
    for (VarSymbol p : parameters) {
      if (ASTHelpers.hasAnnotation(p, FormatString.class, state)) {
        return idx;
      }
      idx++;
    }
    return 0;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    SetMultimap<Symbol, Tree> uses = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
    Map<Symbol, VariableTree> declarations = new LinkedHashMap<>();
    // find calls to String.format and similar where the format string is a private compile-time
    // constant field
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            handle(tree);
            return super.visitMethodInvocation(tree, null);
          }

          private void handle(MethodInvocationTree tree) {
            ExpressionTree arg = formatString(tree, state);
            if (arg == null) {
              return;
            }
            Symbol variable = getSymbol(arg);
            if (variable == null
                || variable.getKind() != ElementKind.FIELD
                || !variable.isPrivate()
                || ASTHelpers.constValue(arg, String.class) == null) {
              return;
            }
            uses.put(variable, arg);
          }
        },
        null);
    // find all uses of the candidate fields, and reject them if they are used outside for calls to
    // format methods
    tree.accept(
        new TreeScanner<Void, Void>() {
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
            if (uses.containsKey(sym) && !uses.containsValue(tree)) {
              uses.removeAll(sym);
            }
          }
        },
        null);
    // find the field declarations
    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VarSymbol sym = getSymbol(tree);
        if (sym != null && uses.containsKey(sym)) {
          declarations.put(sym, tree);
        }
        return super.visitVariable(tree, null);
      }
    }.scan(state.getPath(), null);
    for (Map.Entry<Symbol, Collection<Tree>> e : uses.asMap().entrySet()) {
      Symbol sym = e.getKey();
      Collection<Tree> use = e.getValue();
      VariableTree def = declarations.get(sym);
      if (def == null || !(def.getInitializer() instanceof LiteralTree)) {
        // only inline the constant if its initializer is a literal String
        continue;
      }
      String constValue = state.getSourceForNode(def.getInitializer());
      SuggestedFix.Builder fix = SuggestedFix.builder();
      if (use.size() > 1) {
        // if the format string is used multiple times don't inline at each use-site
        // TODO(cushon): consider suggesting a helper method to do the formatting
        continue;
      }
      fix.delete(def);
      for (Tree u : use) {
        fix.replace(u, constValue);
      }
      state.reportMatch(describeMatch(def, fix.build()));
    }
    return NO_MATCH;
  }
}
