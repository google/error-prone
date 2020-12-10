/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** @author mariasam@google.com (Maria Sam) */
public class FloggerRedundantIsEnabledNegativeCases {

  public void basicCase(FluentLogger logger) {
    logger.atInfo().log("test");
  }

  public void sameLoggerIf(FluentLogger logger, FluentLogger logger2) {
    if (logger.equals(logger2)) {
      logger.atInfo().log("test");
    }
  }

  public void relatedIf(FluentLogger logger, FluentLogger logger2) {
    if (logger.atInfo().isEnabled()) {
      logger2.atInfo().log("test");
    }
  }

  public void doesWork(FluentLogger logger, Map<String, List<String>> map) {
    if (logger.atFine().isEnabled()) {
      for (Map.Entry<String, List<String>> toLog : map.entrySet()) {
        logger.atFine().log("%s%s", toLog.getKey(), Arrays.toString(toLog.getValue().toArray()));
      }
    }
  }

  public void differentLevels(FluentLogger logger) {
    if (logger.atFine().isEnabled()) {
      logger.atInfo().log("This is weird but not necessarily wrong");
    }
  }

  public void checkAtInfo(TestLogger notALogger, FluentLogger logger2) {
    if (notALogger.atInfo().isEnabled()) {
      logger2.atInfo().log("test");
    }
  }

  public void checkAtInfo(TestLogger notALogger) {
    if (notALogger.atInfo() == null) {
      notALogger.atInfo();
    }
  }

  public void checkMethods(FluentLogger logger) {
    if (logger.atInfo().isEnabled()) {
      atInfo();
      isEnabled();
    }
  }

  public void multipleLines(FluentLogger logger) {
    if (logger.atInfo().isEnabled()) {
      int foo = 10;
      logger.atInfo().log("test");
    }
  }

  public boolean atInfo() {
    return true;
  }

  public boolean isEnabled() {
    return true;
  }

  private class TestLogger {
    public TestLogger atInfo() {
      return null;
    }

    public boolean isEnabled() {
      return true;
    }
  }
}
