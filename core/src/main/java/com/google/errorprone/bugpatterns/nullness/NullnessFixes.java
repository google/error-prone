/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
    SuggestedFix.Builder builder = SuggestedFix.builder();
    String qualifiedName = getQualifiedName(state, builder);
    return builder.prefixWith(declaration, "@" + qualifiedName + " ").build();
  }

  private static String getQualifiedName(VisitorState state, SuggestedFix.Builder builder) {
    Symbol sym = FindIdentifiers.findIdent("Nullable", state, KindSelector.VAL_TYP);
    String defaultType =
        state.isAndroidCompatible()
            ? "android.support.annotation.Nullable"
            : "javax.annotation.Nullable";
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
