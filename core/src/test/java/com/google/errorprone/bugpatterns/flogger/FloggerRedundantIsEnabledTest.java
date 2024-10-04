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

package com.google.errorprone.bugpatterns.flogger;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author mariasam@google.com (Maria Sam)
 */
@RunWith(JUnit4.class)
public class FloggerRedundantIsEnabledTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(FloggerRedundantIsEnabled.class, getClass());

  @Test
  public void doPositiveCases() {
    compilationTestHelper
        .addSourceLines(
            "FloggerRedundantIsEnabledPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.flogger.testdata;

            import com.google.common.flogger.FluentLogger;

            /** Created by mariasam on 7/17/17. */
            class FloggerRedundantIsEnabledPositiveCases {

              public void basicCase(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atInfo().isEnabled()) {
                  logger.atInfo().log("test");
                }
              }

              public void nestedIf(FluentLogger logger) {
                if (7 == 7) {
                  // BUG: Diagnostic contains: redundant
                  if (logger.atInfo().isEnabled()) {
                    logger.atInfo().log("test");
                  }
                }
              }

              public void checkBinaryInIf(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (7 == 7 && logger.atInfo().isEnabled()) {
                  logger.atInfo().log("test");
                }
              }

              public void checkBinaryOtherWay(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atInfo().isEnabled() && 7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void complexBinary(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (7 == 7 && (logger != null && logger.atInfo().isEnabled())) {
                  logger.atInfo().log("test");
                }
              }

              public void negated(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (!logger.atInfo().isEnabled()) {
                  logger.atInfo().log("test");
                }
              }

              public void binaryNegated(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (!logger.atInfo().isEnabled() && 7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void checkConfig(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atConfig().isEnabled()) {
                  logger.atConfig().log("test");
                }
              }

              public void checkFine(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atFine().isEnabled()) {
                  logger.atFine().log("test");
                }
              }

              public void checkFiner(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atFiner().isEnabled()) {
                  logger.atFiner().log("test");
                }
              }

              public void checkFinest(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atFinest().isEnabled()) {
                  logger.atFinest().log("test");
                }
              }

              public void checkWarning(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atWarning().isEnabled()) {
                  logger.atWarning().log("test");
                }
              }

              public void checkSevere(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atSevere().isEnabled()) {
                  logger.atSevere().log("test");
                }
              }
            }""")
        .doTest();
  }

  @Test
  public void doNegativeCases() {
    compilationTestHelper
        .addSourceLines(
            "FloggerRedundantIsEnabledNegativeCases.java",
            """
package com.google.errorprone.bugpatterns.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author mariasam@google.com (Maria Sam)
 */
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
}""")
        .doTest();
  }

  @Test
  public void fixes() {
    BugCheckerRefactoringTestHelper.newInstance(FloggerRedundantIsEnabled.class, getClass())
        .addInputLines(
            "FloggerRedundantIsEnabledPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.flogger.testdata;

            import com.google.common.flogger.FluentLogger;

            /** Created by mariasam on 7/17/17. */
            class FloggerRedundantIsEnabledPositiveCases {

              public void basicCase(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atInfo().isEnabled()) {
                  logger.atInfo().log("test");
                }
              }

              public void nestedIf(FluentLogger logger) {
                if (7 == 7) {
                  // BUG: Diagnostic contains: redundant
                  if (logger.atInfo().isEnabled()) {
                    logger.atInfo().log("test");
                  }
                }
              }

              public void checkBinaryInIf(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (7 == 7 && logger.atInfo().isEnabled()) {
                  logger.atInfo().log("test");
                }
              }

              public void checkBinaryOtherWay(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atInfo().isEnabled() && 7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void complexBinary(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (7 == 7 && (logger != null && logger.atInfo().isEnabled())) {
                  logger.atInfo().log("test");
                }
              }

              public void negated(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (!logger.atInfo().isEnabled()) {
                  logger.atInfo().log("test");
                }
              }

              public void binaryNegated(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (!logger.atInfo().isEnabled() && 7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void checkConfig(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atConfig().isEnabled()) {
                  logger.atConfig().log("test");
                }
              }

              public void checkFine(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atFine().isEnabled()) {
                  logger.atFine().log("test");
                }
              }

              public void checkFiner(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atFiner().isEnabled()) {
                  logger.atFiner().log("test");
                }
              }

              public void checkFinest(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atFinest().isEnabled()) {
                  logger.atFinest().log("test");
                }
              }

              public void checkWarning(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atWarning().isEnabled()) {
                  logger.atWarning().log("test");
                }
              }

              public void checkSevere(FluentLogger logger) {
                // BUG: Diagnostic contains: redundant
                if (logger.atSevere().isEnabled()) {
                  logger.atSevere().log("test");
                }
              }
            }""")
        .addOutputLines(
            "FloggerRedundantIsEnabledPositiveCases_expected.java",
            """
            package com.google.errorprone.bugpatterns.flogger.testdata;

            import com.google.common.flogger.FluentLogger;

            /**
             * @author mariasam@google.com (Maria Sam)
             */
            class FloggerRedundantIsEnabledPositiveCases {

              public void basicCase(FluentLogger logger) {
                logger.atInfo().log("test");
              }

              public void nestedIf(FluentLogger logger) {
                if (7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void checkBinaryInIf(FluentLogger logger) {
                if (7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void checkBinaryOtherWay(FluentLogger logger) {
                if (7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void complexBinary(FluentLogger logger) {
                if (7 == 7 && (logger != null)) {
                  logger.atInfo().log("test");
                }
              }

              public void negated(FluentLogger logger) {
                logger.atInfo().log("test");
              }

              public void binaryNegated(FluentLogger logger) {
                if (7 == 7) {
                  logger.atInfo().log("test");
                }
              }

              public void checkConfig(FluentLogger logger) {
                logger.atConfig().log("test");
              }

              public void checkFine(FluentLogger logger) {
                logger.atFine().log("test");
              }

              public void checkFiner(FluentLogger logger) {
                logger.atFiner().log("test");
              }

              public void checkFinest(FluentLogger logger) {
                logger.atFinest().log("test");
              }

              public void checkWarning(FluentLogger logger) {
                logger.atWarning().log("test");
              }

              public void checkSevere(FluentLogger logger) {
                logger.atSevere().log("test");
              }
            }""")
        .doTest(TestMode.AST_MATCH);
  }
}
