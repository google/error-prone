/*
 * Copyright 2018 The Error Prone Authors.
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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

/** @author awturner@google.com (Andy Turner) */
@BugPattern(
  name = "BadNestedImport",
  summary =
      "Importing nested classes with commonly-used names can make code harder to read, because "
          + "it may not be clear from the context exactly which type is being referred to. "
          + "Qualifying the name with that of the containing class can make the code clearer.",
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class BadNestedImport extends BugChecker implements ImportTreeMatcher {
  private static final ImmutableSet<String> IDENTIFIERS_TO_REPLACE = ImmutableSet.of("Builder");

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    if (tree.isStatic()) {
      return Description.NO_MATCH;
    }
    Symbol symbol = ASTHelpers.getSymbol(tree.getQualifiedIdentifier());
    if (symbol == null) {
      return Description.NO_MATCH;
    }
    if (!IDENTIFIERS_TO_REPLACE.contains(symbol.getSimpleName().toString())) {
      return Description.NO_MATCH;
    }

    String enclosingReplacement = "";
    for (Symbol enclosing = symbol.getEnclosingElement();
        enclosing instanceof ClassSymbol;
        enclosing = enclosing.getEnclosingElement()) {
      enclosingReplacement = enclosing.getSimpleName() + "." + enclosingReplacement;
      if (!hasConflictingSymbol(state, enclosing)) {
        return buildDescription(symbol, enclosing, enclosingReplacement, state);
      }
    }
    return Description.NO_MATCH;
  }

  private Description buildDescription(
      Symbol symbol, Symbol enclosing, String enclosingReplacement, VisitorState state) {
    SuggestedFix.Builder builder = SuggestedFix.builder();
    builder.removeImport(symbol.getQualifiedName().toString());
    builder.addImport(enclosing.getQualifiedName().toString());

    IdentifierTree firstFound =
        new TreeScanner<IdentifierTree, Void>() {
          @Override
          public IdentifierTree reduce(IdentifierTree r1, IdentifierTree r2) {
            return (r2 != null) ? r2 : r1;
          }

          @Override
          public IdentifierTree visitIdentifier(IdentifierTree node, Void aVoid) {
            Symbol nodeSymbol = ASTHelpers.getSymbol(node);
            if (symbol.equals(nodeSymbol)) {
              builder.prefixWith(node, enclosingReplacement);
              return node;
            }
            return super.visitIdentifier(node, aVoid);
          }
        }.scan(state.getPath().getCompilationUnit(), null);
    if (firstFound == null) {
      // If no usage of the symbol was found, just leave the import to be cleaned up by the unused
      // import fix.
      return Description.NO_MATCH;
    }
    return describeMatch(firstFound, builder.build());
  }

  private static boolean hasConflictingSymbol(VisitorState state, Symbol parent) {
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    VisitorState checkState;
    if (!compilationUnit.getTypeDecls().isEmpty()) {
      checkState =
          state.withPath(TreePath.getPath(compilationUnit, compilationUnit.getTypeDecls().get(0)));
    } else {
      checkState = state;
    }

    Symbol found =
        FindIdentifiers.findIdent(parent.getSimpleName().toString(), checkState, KindSelector.TYP);
    return found instanceof ClassSymbol && !parent.equals(found);
  }
}
