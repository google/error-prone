/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.analysis;

import com.google.auto.value.AutoValue;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.util.Context;

import java.util.Arrays;
import java.util.Collections;

/**
 * Scans a top-level compilation unit with a local analysis, respecting local suppression
 * annotations.
 *
 * @author Louis Wasserman
 */
@AutoValue
public abstract class LocalAnalysisAsTopLevelAnalysis implements TopLevelAnalysis {
  static LocalAnalysisAsTopLevelAnalysis wrap(LocalAnalysis analysis) {
    return new AutoValue_LocalAnalysisAsTopLevelAnalysis(analysis);
  }

  abstract LocalAnalysis analysis();

  LocalAnalysisAsTopLevelAnalysis() {}

  private boolean suppressed(Tree tree) {
    switch (analysis().suppressibility()) {
      case UNSUPPRESSIBLE:
        return false;
      case CUSTOM_ANNOTATION:
        return ASTHelpers.hasAnnotation(tree, analysis().customSuppressionAnnotation());
      case SUPPRESS_WARNINGS:
        SuppressWarnings suppressions = ASTHelpers.getAnnotation(tree, SuppressWarnings.class);
        return suppressions != null
            && !Collections.disjoint(Arrays.asList(suppressions.value()), analysis().allNames());
      default:
        throw new AssertionError();
    }
  }

  @Override
  public void analyze(CompilationUnitTree compilationUnit, final Context context,
      final AnalysesConfig configuration, DescriptionListener listener) {
    new TreePathScanner<Void, DescriptionListener>() {
      @Override
      public Void scan(Tree tree, DescriptionListener listener) {
        if (!suppressed(tree)) {
          analysis().analyze(getCurrentPath(), context, configuration, listener);
          super.scan(tree, listener);
        }
        return null;
      }
    }.scan(new TreePath(compilationUnit), listener);
  }
}
