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

/** @author mariasam@google.com (Maria Sam) */
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
}
