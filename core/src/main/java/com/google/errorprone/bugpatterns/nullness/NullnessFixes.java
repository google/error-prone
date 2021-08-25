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

import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

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
}
