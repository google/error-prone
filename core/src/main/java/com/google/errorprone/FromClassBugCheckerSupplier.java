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
 * A {@link BugCheckerSupplier} that supplies {@link BugChecker}s given a class.
 */
class FromClassBugCheckerSupplier extends BugCheckerSupplier {
  private final Class<? extends BugChecker> checkerClass;
  private final String canonicalName;
  private final SeverityLevel severity;
  private final MaturityLevel maturity;
  private final Suppressibility suppressibility;

  FromClassBugCheckerSupplier(Class<? extends BugChecker> checkerClass) {
    BugPattern pattern = checkerClass.getAnnotation(BugPattern.class);
    Preconditions.checkArgument(pattern != null,
        "BugChecker class %s must have @BugPattern annotation", checkerClass.getName());
    this.checkerClass = checkerClass;
    this.canonicalName = pattern.name();
    this.severity = pattern.severity();
    this.maturity = pattern.maturity();
    this.suppressibility = pattern.suppressibility();
  }

  private FromClassBugCheckerSupplier(Class<? extends BugChecker> checkerClass,
      String canonicalName, SeverityLevel severity, MaturityLevel maturity,
      Suppressibility suppressibility) {
    this.checkerClass = checkerClass;
    this.canonicalName = canonicalName;
    this.severity = severity;
    this.maturity = maturity;
    this.suppressibility = suppressibility;
  }

  @Override
  public BugChecker get() {
    try {
      BugChecker checker = checkerClass.newInstance();
      checker.setSeverity(severity);
      return checker;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(
          "Could not reflectively create error prone checker " + checkerClass.getName(), e);
    }
  }

  @Override
  public String canonicalName() {
    return canonicalName;
  }

  @Override
  public SeverityLevel severity() {
    return severity;
  }

  @Override
  public BugCheckerSupplier overrideSeverity(SeverityLevel severity) {
    return new FromClassBugCheckerSupplier(
        this.checkerClass, this.canonicalName, severity, this.maturity, this.suppressibility);
  }

  @Override
  public MaturityLevel maturity() {
    return maturity;
  }

  @Override
  public Suppressibility suppressibility() {
    return suppressibility;
  }

  @Override
  public String toString() {
    return "Supplier from class " + checkerClass.getName();
  }
}
