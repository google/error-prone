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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.bugpatterns.BugChecker;

import java.util.Arrays;
import java.util.ServiceLoader;

/**
 * Utility class to generate a {@code TopLevelAnalysis} from error-prone BugCheckers.
 *
 * @author Louis Wasserman
 */
public class ErrorProneTopLevelAnalysis {

  /**
   * Wraps a BugChecker with the specified maturity level as a TopLevelAnalysis.
   */
  private static TopLevelAnalysis create(BugChecker checker, MaturityLevel maturity) {
    return TopLevelAnalysisWithSeverity.wrap(checker.canonicalName(), 
        maturity == MaturityLevel.MATURE,
        checker.defaultSeverity(),
        checker.suppressibility().disableable(),
        LocalAnalysisAsTopLevelAnalysis.wrap(new BugCheckerLocalAnalysis(checker)));
  }

  /**
   * Wraps a {@code BugChecker} as a {@code TopLevelAnalysis}, but overrides the default maturity
   * level.
   */
  public static TopLevelAnalysis createMature(BugChecker checker) {
    return create(checker, MaturityLevel.MATURE);
  }

  /**
   * Returns a {@code TopLevelAnalysis} using the specified checkers at their default maturity
   * level.
   */
  public static TopLevelAnalysis create(BugChecker... checkers) {
    return create(Arrays.asList(checkers));
  }

  /**
   * Returns a {@code TopLevelAnalysis} using the specified checkers at their default maturity
   * level.
   */
  public static TopLevelAnalysis create(Iterable<? extends BugChecker> checkers) {
    ImmutableList.Builder<TopLevelAnalysis> analyses = ImmutableList.builder();
    for (BugChecker checker : checkers) {
      analyses.add(create(checker, checker.maturity()));
    }
    return SumTopLevelAnalysis.create(analyses.build());
  }

  /**
   * Returns a {@code TopLevelAnalysis} using the specified checkers at MATURE maturity
   * level.
   */
  public static TopLevelAnalysis createMature(Iterable<? extends BugChecker> checkers) {
    ImmutableList.Builder<TopLevelAnalysis> analyses = ImmutableList.builder();
    for (BugChecker checker : checkers) {
      analyses.add(create(checker, MaturityLevel.MATURE));
    }
    return SumTopLevelAnalysis.create(analyses.build());
  }

  private static final Function<Class<? extends BugChecker>, BugChecker> INSTANTIATE_CHECKER =
      new Function<Class<? extends BugChecker>, BugChecker>() {
        @Override
        public BugChecker apply(Class<? extends BugChecker> checkerClass) {
          try {
            return checkerClass.newInstance();
          } catch (InstantiationException | IllegalAccessException e) {
            throw new LinkageError("Could not instantiate BugChecker.", e);
          }
        }
      };

  /**
   * Returns a {@code TopLevelAnalysis} using the specified checkers at their default maturity
   * level.
   */
  public static TopLevelAnalysis createFromClasses(
      Iterable<? extends Class<? extends BugChecker>> classes) {
    return create(Iterables.transform(classes, INSTANTIATE_CHECKER));
  }

  /**
   * Returns a {@code TopLevelAnalysis} using the specified checkers, all at MATURE.
   */
  public static TopLevelAnalysis createMatureFromClasses(
      Iterable<? extends Class<? extends BugChecker>> classes) {
    return createMature(Iterables.transform(classes, INSTANTIATE_CHECKER));
  }
  
  /**
   * Returns a {@code TopLevelAnalysis} using checkers loaded from a {@code ServiceLoader}.
   */
  public static TopLevelAnalysis createFromServiceLoader() {
    return create(bugCheckerLoader);
  }

  private static final ServiceLoader<BugChecker> bugCheckerLoader =
      ServiceLoader.load(BugChecker.class);

  private ErrorProneTopLevelAnalysis() {}
}
