/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;
import static javax.lang.model.element.ElementKind.PARAMETER;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Static utility methods for common functionality in the nullable checkers.
 *
 * @author awturner@google.com (Andy Turner)
 */
class NullnessFixes {
  private NullnessFixes() {}

  /** Make the {@link SuggestedFix} to add the {@code Nullable} annotation. */
  static SuggestedFix makeFix(VisitorState state, Tree declaration) {
    // TODO(cpovirk): Remove any @NonNull, etc. annotation that is present?
    SuggestedFix.Builder builder = SuggestedFix.builder();
    String qualifiedName = getQualifiedName(state, builder);
    return builder.prefixWith(declaration, "@" + qualifiedName + " ").build();
  }

  private static String getQualifiedName(VisitorState state, SuggestedFix.Builder builder) {
    /*
     * TODO(cpovirk): Instead of hardcoding these two annotations, pick the one that seems most
     * appropriate for each user:
     *
     * - Look for usages in other files in the compilation?
     *
     * - Look for imports of other annotations that are part of an artifact that also contains
     *   @Nullable (e.g., javax.annotation.Nonnull).
     *
     * - Call getSymbolFromString. (But that may return transitive dependencies that will cause
     *   compilation to fail strict-deps checking.)
     *
     * - Among available candidates, prefer type-usage annotations.
     *
     * - When we suggest a jsr305 annotation, might we want to suggest @CheckForNull over @Nullable?
     *   It's more verbose, but it's more obviously a declaration annotation, and it's the
     *   annotation that is *technically* defined to produce the behaviors that users want.
     *
     * TODO(cpovirk): This code will probably already use type annotations if they are imported, but
     * it probably isn't always putting them in the right place for arrays and nested types.
     */
    // TODO(cpovirk): Suggest @NullableDecl if the code uses that.
    Symbol sym = FindIdentifiers.findIdent("Nullable", state, KindSelector.VAL_TYP);
    ErrorProneFlags flags = state.errorProneOptions().getFlags();
    String defaultType =
        flags
            .get("Nullness:DefaultNullnessAnnotation")
            .orElse(
                state.isAndroidCompatible()
                    ? "androidx.annotation.Nullable"
                    : "javax.annotation.Nullable");
    if (sym != null) {
      ClassSymbol classSym = (ClassSymbol) sym;
      if (classSym.isAnnotationType()) {
        // We've got an existing annotation called Nullable. We can use this.
        return "Nullable";
      } else {
        // It's not an annotation type. We have to fully-qualify the import.
        return defaultType;
      }
    }
    // There is no symbol already. Import and use.
    builder.addImport(defaultType);
    return "Nullable";
  }

  static BareIdentifierNullCheck getBareIdentifierNullCheck(ExpressionTree tree) {
    tree = stripParentheses(tree);

    BinaryTree equalityTree = (BinaryTree) tree;
    ExpressionTree nullChecked;
    if (equalityTree.getRightOperand().getKind() == NULL_LITERAL) {
      nullChecked = equalityTree.getLeftOperand();
    } else if (equalityTree.getLeftOperand().getKind() == NULL_LITERAL) {
      nullChecked = equalityTree.getRightOperand();
    } else {
      return null;
    }

    if (nullChecked.getKind() != IDENTIFIER) {
      return null;
    }

    Symbol symbol = getSymbol(nullChecked);
    VarSymbol locallyDefinedSymbol =
        symbol != null && LOCALLY_DEFINED_ELEMENT_KINDS.contains(symbol.getKind())
            ? (VarSymbol) symbol
            : null;

    return new AutoValue_NullnessFixes_BareIdentifierNullCheck(locallyDefinedSymbol);
  }

  /**
   * A check of a bare identifier against {@code null}, like {@code foo == null}.
   *
   * <p>We restrict ourselves to bare identifiers because it's easy and safe. The obvious easy
   * alternative would be to accept any value for which we can get a {@link Symbol}. However, using
   * {@code Symbol} might lead code to assume that a null check of {@code foo.bar} guarantees
   * something about {@code otherFoo.bar}, which is represented by the same symbol.
   *
   * <p>Even with this restriction, callers should be wary when examining code that might:
   *
   * <ul>
   *   <li>assign a new value to the identifier after the null check but before some usage
   *   <li>declare a new identifier that hides the old
   * </ul>
   *
   * TODO(cpovirk): Consider looking for more than just bare identifiers. For example, we could
   * probably assume that a null check of {@code foo.bar} ensures that {@code foo.bar} is non-null
   * in the future. One case that might be particularly useful is {@code this.bar}. We might even go
   * further, assuming that {@code foo.bar()} will continue to have the same value.
   */
  @AutoValue
  abstract static class BareIdentifierNullCheck {
    /**
     * Returns the symbol that was checked against null but only if it was a local variable or
     * parameter.
     *
     * <p>This restriction avoids the problems discussed in the class documentation, but it comes at
     * the cost of not handling fields.
     */
    @Nullable
    abstract VarSymbol locallyDefinedSymbol();
  }

  private static final ImmutableSet<ElementKind> LOCALLY_DEFINED_ELEMENT_KINDS =
      ImmutableSet.of(LOCAL_VARIABLE, PARAMETER);
}
