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

import com.google.errorprone.DescriptionListener;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.util.Context;

import java.util.Set;

/**
 * Represents an analysis that can be run on an entire compilation unit.
 *
 * @author Louis Wasserman
 */
public interface TopLevelAnalysis {
  /**
   * Analyzes the specified compilation unit with the given javac context, configuration, and
   * callback for static analysis results.
   */
  void analyze(CompilationUnitTree compilationUnit, Context context, AnalysesConfig configuration,
      DescriptionListener listener);

  /**
   * Returns the set of all analyses in this {@code TopLevelAnalysis}.
   */
  Set<String> knownAnalysisNames();
}
