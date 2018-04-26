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
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
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
public class BadNestedImport extends BugChecker implements IdentifierTreeMatcher {
  private static final ImmutableSet<String> IDENTIFIERS_TO_REPLACE =
      ImmutableSet.of("Builder", "Type", "Entry");

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (!(symbol instanceof ClassSymbol)) {
      return Description.NO_MATCH;
    }
    if (!IDENTIFIERS_TO_REPLACE.contains(symbol.getSimpleName().toString())) {
      return Description.NO_MATCH;
    }
    if (!findImport(symbol, state)) {
      return Description.NO_MATCH;
    }

    Symbol parent = symbol.getEnclosingElement();
    if (!(parent instanceof ClassSymbol)) {
      return Description.NO_MATCH;
    }

    Symbol found = FindIdentifiers.findIdent(parent.getSimpleName().toString(), state);
    if (found instanceof ClassSymbol && !parent.equals(found)) {
      // There is another symbol already with this name.
      return Description.NO_MATCH;
    }

    return describeMatch(
        tree,
        SuggestedFix.builder()
            .addImport(parent.toString())
            .replace(tree, parent.getSimpleName() + "." + symbol.getSimpleName())
            .build());
  }

  private static boolean findImport(Symbol symbol, VisitorState state) {
    // Inspect the imports to make sure we actually are importing this name.
    CompilationUnitTree compilationUnitTree = state.findEnclosing(CompilationUnitTree.class);
    if (compilationUnitTree == null) {
      // Something is very odd if we hit this... just don't bail.
      return false;
    }

    for (ImportTree importTree : compilationUnitTree.getImports()) {
      Symbol imported = ASTHelpers.getSymbol(importTree.getQualifiedIdentifier());
      if (symbol.equals(imported)) {
        // We are importing this identifier, so we want to stop doing that.
        return true;
      }
    }

    return false;
  }
}
