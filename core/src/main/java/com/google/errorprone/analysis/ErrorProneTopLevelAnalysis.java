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

import com.google.errorprone.bugpatterns.BugChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Utility class to generate a {@code TopLevelAnalysis} containing all error-prone BugCheckers
 * available from ServiceLoader.
 * 
 * @author Louis Wasserman
 */
public class ErrorProneTopLevelAnalysis {
  private static final ServiceLoader<BugChecker> bugCheckerLoader =
      ServiceLoader.load(BugChecker.class);

  public static TopLevelAnalysis create() {
    List<TopLevelAnalysis> analyses = new ArrayList<>();
    for (BugChecker checker : bugCheckerLoader) {
      analyses.add(TopLevelAnalysisWithSeverity.wrap(checker.canonicalName(),
          checker.defaultSeverity(), checker.suppressibility().disableable(),
          LocalAnalysisAsTopLevelAnalysis.wrap(new BugCheckerLocalAnalysis(checker))));
    }
    return SumTopLevelAnalysis.create(analyses);
  }

  private ErrorProneTopLevelAnalysis() {}
}
