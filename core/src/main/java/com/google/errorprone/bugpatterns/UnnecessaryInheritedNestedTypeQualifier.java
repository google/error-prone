/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.FindIdentifiers.findIdent;
import static com.sun.tools.javac.code.Kinds.KindSelector.TYP;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

/**
 * A {@link BugChecker} that detects unnecessary qualification of nested types that are inherited
 * from a superclass.
 *
 * <p>For example, instead of {@code Super.Nested} inside a subclass {@code Sub extends Super}, just
 * {@code Nested} can be used, provided it doesn't conflict with another symbol of the same name.
 */
@BugPattern(summary = "Unnecessary qualifying of inherited nested types.", severity = WARNING)
public final class UnnecessaryInheritedNestedTypeQualifier extends BugChecker
    implements MemberSelectTreeMatcher {

  @Override
  public Description matchMemberSelect(MemberSelectTree memberSelectTree, VisitorState state) {
    // Verify the resolved symbol is a nested class.
    Symbol symbol = getSymbol(memberSelectTree);
    if (!(symbol instanceof ClassSymbol nestedClassSym)) {
      return NO_MATCH;
    }

    // Verify the qualifier is also a class.
    Symbol qualifierSym = getSymbol(memberSelectTree.getExpression());
    if (!(qualifierSym instanceof ClassSymbol qualifierClassSym)) {
      return NO_MATCH;
    }

    // Find the enclosing class of the usage site.
    // TODO(kak): Support nested classes of subclasses (by walking up the TreePath).
    ClassTree enclosingClassTree = findEnclosingNode(state.getPath(), ClassTree.class);
    if (enclosingClassTree == null) {
      return NO_MATCH;
    }
    ClassSymbol enclosingClassSym = getSymbol(enclosingClassTree);
    if (enclosingClassSym == null) {
      return NO_MATCH;
    }

    // Find the owner (declaring class) of the nested class.
    Symbol ownerSym = nestedClassSym.owner;
    if (!(ownerSym instanceof ClassSymbol ownerClassSym)) {
      return NO_MATCH;
    }

    // Check if Nested is inherited by Enclosing (i.e. Enclosing is a strict subclass of Owner)
    if (enclosingClassSym.equals(ownerClassSym)
        || !isSubtype(enclosingClassSym.type, ownerClassSym.type, state)) {
      return NO_MATCH;
    }

    // Check if Qualifier is in the inheritance hierarchy between Enclosing and Owner
    if (!isSubtype(enclosingClassSym.type, qualifierClassSym.type, state)
        || !isSubtype(qualifierClassSym.type, ownerClassSym.type, state)) {
      return NO_MATCH;
    }

    // Check if the simple name resolves to the same symbol without qualification.
    // This ensures that removing the qualifier won't cause shadowing conflicts.
    String simpleName = nestedClassSym.getSimpleName().toString();

    // We use KindSelector.TYP to restrict the search to type symbols (e.g. classes,
    // interfaces), ignoring variables or methods with the same name.
    Symbol found = findIdent(simpleName, state, TYP);
    if (found == null || !found.equals(nestedClassSym)) {
      return NO_MATCH;
    }

    // Warn and suggest fix
    return buildDescription(memberSelectTree)
        .addFix(SuggestedFix.replace(memberSelectTree, simpleName))
        .setMessage(
            String.format(
                "The qualifier '%s' is unnecessary because the nested type '%s' is inherited.",
                qualifierClassSym.getSimpleName(), simpleName))
        .build();
  }
}
