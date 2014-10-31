/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.common.base.Preconditions;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;

/**
 * A {@link BugCheckerSupplier} that supplies the {@link BugChecker} instance that was passed
 * in its constructor.
 */
class FromInstanceBugCheckerSupplier extends BugCheckerSupplier {
  private final BugChecker checker;

  FromInstanceBugCheckerSupplier(BugChecker checker) {
    this.checker = Preconditions.checkNotNull(checker);
  }

  @Override
  public BugChecker get() {
    return checker;
  }

  @Override
  public String canonicalName() {
    return checker.canonicalName();
  }

  @Override
  public SeverityLevel severity() {
    return checker.severity();
  }

  @Override
  public MaturityLevel maturity() {
    return checker.maturity();
  }

  @Override
  public boolean disableable() {
    return checker.disableable();
  }
}
