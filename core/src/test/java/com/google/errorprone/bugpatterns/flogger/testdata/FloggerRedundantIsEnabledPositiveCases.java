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
}
