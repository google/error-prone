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
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.util.Context;
import java.util.Set;

/**
 * Wraps a {@code TopLevelAnalysis} to handle severity levels, taking into account
 * {@code ErrorProneOptions} in updating the severity level.
 *
 * @author Louis Wasserman
 */
@AutoValue
public abstract class TopLevelAnalysisWithSeverity implements TopLevelAnalysis {
  static TopLevelAnalysisWithSeverity wrap(String canonicalName, boolean onByDefault,
      SeverityLevel defaultSeverity, boolean disableable, TopLevelAnalysis analysis) {
    return new AutoValue_TopLevelAnalysisWithSeverity(canonicalName, onByDefault, defaultSeverity,
        disableable, analysis);
  }

  // TODO(lowasser): consider eliminating this and respecting all configs on knownAnalysisNames
  abstract String canonicalName();

  abstract boolean onByDefault();

  abstract SeverityLevel defaultSeverity();

  abstract boolean disableable();

  abstract TopLevelAnalysis analysis();

  @Override
  public Set<String> knownAnalysisNames() {
    return analysis().knownAnalysisNames();
  }

  @Override
  public void analyze(CompilationUnitTree compilationUnit, Context context,
      AnalysesConfig config, final DescriptionListener listener) {
    final SeverityLevel severity;
    boolean on = onByDefault();
    Severity optionsSeverity = config.errorProneOptions().getSeverityMap().get(canonicalName());
    if (optionsSeverity != null) {
      switch (optionsSeverity) {
        case OFF:
          if (!disableable()) {
            throw new IllegalArgumentException(canonicalName() + " may not be disabled");
          }
          on = false;
          severity = SeverityLevel.SUGGESTION;
          break;
        case DEFAULT:
          on = true;
          severity = defaultSeverity();
          break;
        case WARN:
          // Demoting an enabled check from an error to a warning is a form of disabling
          if (!disableable() && defaultSeverity() == SeverityLevel.ERROR) {
            throw new IllegalArgumentException(
                canonicalName() + " is not disableable and may not be demoted to a warning");
          }
          on = true;
          severity = SeverityLevel.WARNING;
          break;
        case ERROR:
          on = true;
          severity = SeverityLevel.ERROR;
          break;
        default:
          throw new IllegalStateException("Unexpected severity level: " + optionsSeverity);
      }
    } else {
      severity = defaultSeverity();
    }
    if (on) {
      analysis().analyze(compilationUnit, context, config, new DescriptionListener() {
        @Override
        public void onDescribed(Description description) {
          listener.onDescribed(description.applySeverityOverride(severity));
        }
      });
    }
  }
}
