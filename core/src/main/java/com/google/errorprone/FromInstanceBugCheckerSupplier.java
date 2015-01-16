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
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.bugpatterns.BugChecker;

/**
 * A {@link BugCheckerSupplier} that supplies the {@link BugChecker} instance that was passed
 * in its constructor.
 */
class FromInstanceBugCheckerSupplier extends BugCheckerSupplier {
  private final BugChecker checker;
  private final SeverityLevel severity;

  FromInstanceBugCheckerSupplier(BugChecker checker) {
    this(Preconditions.checkNotNull(checker), checker.severity());
  }

  private FromInstanceBugCheckerSupplier(BugChecker checker, SeverityLevel severity) {
    this.checker = checker;
    this.severity = severity;
  }

  @Override
  public BugChecker get() {
    /* Note that we mutate the severity of the BugChecker instance here, which is not ideal.
     * Ideally BugChecker instances would be immutable, and we would ask for a copy of this
     * BugChecker with a different severity.
     *
     * Instead, we make BugCheckerSupplier and ScannerSupplier immutable. When we process severity
     * overrides, we do not store a reference to the BugCheckerSupplier with the overridden
     * severity; we keep only the original BugCheckerSupplier.  This ensures that the default
     * severity is used for subsequent compilations.
     */
    checker.setSeverity(severity);
    return checker;
  }

  @Override
  public String canonicalName() {
    return checker.canonicalName();
  }

  @Override
  public SeverityLevel severity() {
    return severity;
  }

  @Override
  public BugCheckerSupplier overrideSeverity(SeverityLevel severity) {
    return new FromInstanceBugCheckerSupplier(this.checker, severity);
  }

  @Override
  public MaturityLevel maturity() {
    return checker.maturity();
  }

  @Override
  public Suppressibility suppressibility() {
    return checker.suppressibility();
  }
}
