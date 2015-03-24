/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.auto.value.AutoValue;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.code.Type;

import javax.annotation.Nullable;

/**
 * Static imports shouldn't be used for types.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "UnnecessaryStaticImport",
    summary = "Using static imports for types is unnecessary",
    explanation = "Using static imports for types is unnecessary, since they can always be"
        + " replaced by equivalent non-static imports.",
    category = JDK, severity = WARNING, maturity = MATURE)
public class UnnecessaryStaticImport extends BugChecker implements ImportTreeMatcher {

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    StaticTypeImportInfo importInfo = StaticTypeImportInfo.tryCreate(tree, state);
    if (importInfo == null) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(tree, importInfo.importStatement()));
  }

  /**
   * Represents a static single-type import.
   */
  @AutoValue
  abstract static class StaticTypeImportInfo {

    /** @return the imported type */
    abstract Type type();

    /** @return the fully qualified name used to import the type (possibly non-canonical) */
    abstract String importedName();

    /** @return the fully-qualified canonical name of the type */
    abstract String canonicalName();

    /**
     * Returns true if the import is canonical, i.e. the fully qualified name used to import the
     * type matches the scopes it was declared in.
     */
    boolean isCanonical() {
      return canonicalName().equals(importedName());
    }

    /** Builds the canonical import statement for the type. */
    String importStatement() {
      return String.format("import %s;", canonicalName());
    }

    /**
     * Returns a {@link StaticTypeImportInfo} if the given import is a static single-type import.
     * Returns {@code null} otherwise, e.g. because the import is non-static, or an on-demand
     * import, or statically imports a field or method.
     */
    @Nullable
    static StaticTypeImportInfo tryCreate(ImportTree tree, VisitorState state) {
      if (!tree.isStatic()) {
        return null;
      }
      String importedName = tree.getQualifiedIdentifier().toString();
      Type result = state.getTypeFromString(importedName);
      if (result == null) {
        return null;
      }
      String canonicalName = state.getTypes().erasure(result).toString();
      if (canonicalName == null) {
        return null;
      }
      return new AutoValue_UnnecessaryStaticImport_StaticTypeImportInfo(
          result, importedName, canonicalName);
    }
  }
}

