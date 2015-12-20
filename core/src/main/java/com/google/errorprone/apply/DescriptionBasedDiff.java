/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.Replacement;
import com.google.errorprone.matchers.Description;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of a {@link Diff} that performs the modifications that are passed to its
 * {@link #onDescribed} method, with no formatting.
 * 
 * <p>If imports are changed, they are resorted as per Google Java style.
 * 
 * @author lowasser@google.com (Louis Wasserman)
 */
public final class DescriptionBasedDiff implements DescriptionListener, Diff {
  private final String sourcePath;
  private final JCCompilationUnit compilationUnit;
  private final Set<String> importsToAdd;
  private final Set<String> importsToRemove;
  private final EndPosTable endPositions;
  private final RangeMap<Integer, Replacement> replacements;

  public static DescriptionBasedDiff create(JCCompilationUnit compilationUnit) {
    return new DescriptionBasedDiff(compilationUnit);
  }

  private DescriptionBasedDiff(JCCompilationUnit compilationUnit) {
    this.compilationUnit = checkNotNull(compilationUnit);
    this.sourcePath = compilationUnit.getSourceFile().toUri().getPath();
    this.importsToAdd = new HashSet<>();
    this.importsToRemove = new HashSet<>();
    this.endPositions = compilationUnit.endPositions;
    this.replacements = TreeRangeMap.create();
  }

  @Override
  public String getRelevantFileName() {
    return sourcePath;
  }
  
  public boolean isEmpty() {
    return importsToAdd.isEmpty() && importsToRemove.isEmpty()
        && replacements.asMapOfRanges().isEmpty();
  }

  @Override
  public void onDescribed(Description description) {
    // Use only first (most likely) suggested fix
    if (description.fixes.size() > 0) {
      Fix fix = description.fixes.get(0);
      importsToAdd.addAll(fix.getImportsToAdd());
      importsToRemove.addAll(fix.getImportsToRemove());
      for (Replacement replacement : fix.getReplacements(endPositions)) {
        addReplacement(replacement);
      }
    }
  }

  private void addReplacement(Replacement replacement) {
    checkNotNull(replacement);
    Range<Integer> range = Range.closedOpen(replacement.startPosition(), replacement.endPosition());

    RangeMap<Integer, Replacement> overlaps = replacements.subRangeMap(range);
    checkArgument(overlaps.asMapOfRanges().isEmpty(), "Replacement %s overlaps with %s",
        replacement, overlaps);

    replacements.put(range, replacement);
  }

  @Override
  public void applyDifferences(SourceFile sourceFile) throws DiffNotApplicableException {
    /*
     * We want to apply replacements in reverse order of start position, and we know that imports
     * come before all the other replacements.
     */
    List<Replacement> replacementsInOrder = new ArrayList<>(replacements.asMapOfRanges().values());
    Collections.reverse(replacementsInOrder);
    if (!importsToAdd.isEmpty() || !importsToRemove.isEmpty()) {
      ImportStatements importStatements = ImportStatements.create(compilationUnit);
      importStatements.addAll(importsToAdd);
      importStatements.removeAll(importsToRemove);
      replacementsInOrder.add(Replacement.create(importStatements.getStartPos(),
          importStatements.getEndPos(), importStatements.toString()));
    }

    for (Replacement replacement : replacementsInOrder) {
      sourceFile.replaceChars(replacement.startPosition(), replacement.endPosition(),
          replacement.replaceWith());
    }
  }
}
