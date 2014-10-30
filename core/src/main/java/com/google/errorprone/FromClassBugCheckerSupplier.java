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
 * A {@link BugCheckerSupplier} that supplies {@link BugChecker}s given a class.
 */
class FromClassBugCheckerSupplier extends BugCheckerSupplier {
  private final Class<? extends BugChecker> checkerClass;
  private final String canonicalName;
  private final SeverityLevel severity;
  private final MaturityLevel maturity;
  private final boolean disableable;

  FromClassBugCheckerSupplier(Class<? extends BugChecker> checkerClass) {
    BugPattern pattern = checkerClass.getAnnotation(BugPattern.class);
    Preconditions.checkArgument(pattern != null,
        "BugChecker class %s must have @BugPattern annotation", checkerClass.getName());
    this.checkerClass = checkerClass;
    this.canonicalName = pattern.name();
    this.severity = pattern.severity();
    this.maturity = pattern.maturity();
    this.disableable = pattern.disableable();
  }

  @Override
  public BugChecker get() {
    try {
      return checkerClass.newInstance();
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
  public MaturityLevel maturity() {
    return maturity;
  }

  @Override
  public boolean disableable() {
    return disableable;
  }
}
