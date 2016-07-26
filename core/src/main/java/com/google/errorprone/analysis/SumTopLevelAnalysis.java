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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.DescriptionListener;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.util.Context;
import java.util.Set;

/**
 * Represents a combination of multiple top-level analyses.
 *
 * @author Louis Wasserman
 */
@AutoValue
public abstract class SumTopLevelAnalysis implements TopLevelAnalysis {
  public static SumTopLevelAnalysis create(Iterable<? extends TopLevelAnalysis> analyses) {
    return new AutoValue_SumTopLevelAnalysis(ImmutableList.copyOf(analyses));
  }

  SumTopLevelAnalysis() {}

  abstract ImmutableList<TopLevelAnalysis> analyses();

  @Override
  public Set<String> knownAnalysisNames() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (TopLevelAnalysis analysis : analyses()) {
      builder.addAll(analysis.knownAnalysisNames());
    }
    return builder.build();
  }

  @Override
  public void analyze(CompilationUnitTree compilationUnit, Context context,
      AnalysesConfig config, DescriptionListener listener) {
    for (TopLevelAnalysis analysis : analyses()) {
      analysis.analyze(compilationUnit, context, config, listener);
    }
  }
}
