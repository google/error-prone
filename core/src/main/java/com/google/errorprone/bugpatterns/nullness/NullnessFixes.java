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

import static com.google.errorprone.bugpatterns.nullness.NullnessFixes.NullCheck.Polarity.IS_NOT_NULL;
import static com.google.errorprone.bugpatterns.nullness.NullnessFixes.NullCheck.Polarity.IS_NULL;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.nullness.NullnessFixes.NullCheck.Polarity;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.annotation.Nullable;
import javax.lang.model.element.Name;

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

  @Nullable
  static NullCheck getNullCheck(ExpressionTree tree) {
    tree = stripParentheses(tree);

    Polarity polarity;
    switch (tree.getKind()) {
      case EQUAL_TO:
        polarity = IS_NULL;
        break;
      case NOT_EQUAL_TO:
        polarity = IS_NOT_NULL;
        break;
      default:
        return null;
    }

    BinaryTree equalityTree = (BinaryTree) tree;
    ExpressionTree nullChecked;
    if (equalityTree.getRightOperand().getKind() == NULL_LITERAL) {
      nullChecked = equalityTree.getLeftOperand();
    } else if (equalityTree.getLeftOperand().getKind() == NULL_LITERAL) {
      nullChecked = equalityTree.getRightOperand();
    } else {
      return null;
    }

    Name name =
        nullChecked.getKind() == IDENTIFIER ? ((IdentifierTree) nullChecked).getName() : null;

    Symbol symbol = getSymbol(nullChecked);
    VarSymbol varSymbol = symbol instanceof VarSymbol ? (VarSymbol) symbol : null;

    return new AutoValue_NullnessFixes_NullCheck(name, varSymbol, polarity);
  }

  /**
   * A check of a variable against {@code null}, like {@code foo == null}.
   *
   * <p>This class exposes the variable in two forms: the {@link VarSymbol} (if available) and the
   * {@link Name} (if the null check was performed on a bare identifier, like {@code foo}). Many
   * callers restrict themselves to bare identifiers because it's easy and safe: Using {@code
   * Symbol} might lead code to assume that a null check of {@code foo.bar} guarantees something
   * about {@code otherFoo.bar}, which is represented by the same symbol.
   *
   * <p>Even when restricting themselves to bare identifiers, callers should be wary when examining
   * code that might:
   *
   * <ul>
   *   <li>assign a new value to the identifier after the null check but before some usage
   *   <li>declare a new identifier that hides the old
   * </ul>
   *
   * TODO(cpovirk): What our callers really care about is not "bare identifiers" but "this
   * particular 'instance' of a variable," so we could generalize to cover more cases of that. For
   * example, we could probably assume that a null check of {@code foo.bar} ensures that {@code
   * foo.bar} is non-null in the future. One case that might be particularly useful is {@code
   * this.bar}. We might even go further, assuming that {@code foo.bar()} will continue to have the
   * same value in some cases.
   */
  @com.google.auto.value.AutoValue // fully qualified to work around JDK-7177813(?) in JDK8 build
  abstract static class NullCheck {
    /**
     * Returns the bare identifier that was checked against {@code null}, if the null check took
     * that form. Prefer this over {@link #varSymbolButUsuallyPreferBareIdentifier} in most cases,
     * as discussed in the class documentation.
     */
    @Nullable
    abstract Name bareIdentifier();

    /** Returns the symbol that was checked against {@code null}. */
    @Nullable
    abstract VarSymbol varSymbolButUsuallyPreferBareIdentifier();

    abstract Polarity polarity();

    boolean bareIdentifierMatches(ExpressionTree other) {
      return other.getKind() == IDENTIFIER
          && bareIdentifier() != null
          && bareIdentifier().equals(((IdentifierTree) other).getName());
    }

    ExpressionTree nullCase(ConditionalExpressionTree tree) {
      return polarity() == IS_NULL ? tree.getTrueExpression() : tree.getFalseExpression();
    }

    StatementTree nullCase(IfTree tree) {
      return polarity() == IS_NULL ? tree.getThenStatement() : tree.getElseStatement();
    }

    enum Polarity {
      IS_NULL,
      IS_NOT_NULL,
    }
  }
}
