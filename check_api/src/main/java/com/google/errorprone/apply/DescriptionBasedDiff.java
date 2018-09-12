/*
 * Copyright 2014 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.DescriptionListener;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.Replacement;
import com.google.errorprone.fixes.Replacements;
import com.google.errorprone.matchers.Description;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.net.URI;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of a {@link Diff} that performs the modifications that are passed to its {@link
 * #onDescribed} method, with no formatting.
 *
 * <p>If imports are changed, they are resorted as per Google Java style.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public final class DescriptionBasedDiff implements DescriptionListener, Diff {

  private final String sourcePath;
  private final boolean ignoreOverlappingFixes;
  private final JCCompilationUnit compilationUnit;
  private final Set<String> importsToAdd;
  private final Set<String> importsToRemove;
  private final EndPosTable endPositions;
  private final Replacements replacements = new Replacements();
  private final ImportOrganizer importOrganizer;

  public static DescriptionBasedDiff create(
      JCCompilationUnit compilationUnit, ImportOrganizer importOrganizer) {
    return new DescriptionBasedDiff(compilationUnit, false, importOrganizer);
  }

  public static DescriptionBasedDiff createIgnoringOverlaps(
      JCCompilationUnit compilationUnit, ImportOrganizer importOrganizer) {
    return new DescriptionBasedDiff(compilationUnit, true, importOrganizer);
  }

  private DescriptionBasedDiff(
      JCCompilationUnit compilationUnit,
      boolean ignoreOverlappingFixes,
      ImportOrganizer importOrganizer) {
    this.compilationUnit = checkNotNull(compilationUnit);
    URI sourceFileUri = compilationUnit.getSourceFile().toUri();
    this.sourcePath =
        (sourceFileUri.isAbsolute() && Objects.equals(sourceFileUri.getScheme(), "file"))
            ? Paths.get(sourceFileUri).toAbsolutePath().toString()
            : sourceFileUri.getPath();
    this.ignoreOverlappingFixes = ignoreOverlappingFixes;
    this.importsToAdd = new LinkedHashSet<>();
    this.importsToRemove = new LinkedHashSet<>();
    this.endPositions = compilationUnit.endPositions;
    this.importOrganizer = importOrganizer;
  }

  @Override
  public String getRelevantFileName() {
    return sourcePath;
  }

  public boolean isEmpty() {
    return importsToAdd.isEmpty() && importsToRemove.isEmpty() && replacements.isEmpty();
  }

  @Override
  public void onDescribed(Description description) {
    // Use only first (most likely) suggested fix
    if (description.fixes.size() > 0) {
      handleFix(description.fixes.get(0));
    }
  }

  public void handleFix(Fix fix) {
    importsToAdd.addAll(fix.getImportsToAdd());
    importsToRemove.addAll(fix.getImportsToRemove());
    for (Replacement replacement : fix.getReplacements(endPositions)) {
      try {
        replacements.add(replacement, Replacements.CoalescePolicy.EXISTING_FIRST);
      } catch (IllegalArgumentException iae) {
        if (!ignoreOverlappingFixes) {
          throw iae;
        }
      }
    }
  }

  @Override
  public void applyDifferences(SourceFile sourceFile) throws DiffNotApplicableException {
    if (!importsToAdd.isEmpty() || !importsToRemove.isEmpty()) {
      ImportStatements importStatements = ImportStatements.create(compilationUnit, importOrganizer);
      importStatements.addAll(importsToAdd);
      importStatements.removeAll(importsToRemove);
      if (importStatements.importsHaveChanged()) {
        replacements.add(
            Replacement.create(
                importStatements.getStartPos(),
                importStatements.getEndPos(),
                importStatements.toString()),
            Replacements.CoalescePolicy.REPLACEMENT_FIRST);
      }
    }
    for (Replacement replacement : replacements.descending()) {
      sourceFile.replaceChars(
          replacement.startPosition(), replacement.endPosition(), replacement.replaceWith());
    }
  }
}
