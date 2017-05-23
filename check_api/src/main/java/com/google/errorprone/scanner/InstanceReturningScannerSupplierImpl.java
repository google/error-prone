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

package com.google.errorprone.scanner;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneFlags;

/**
 * An implementation of a {@link ScannerSupplier} that just returns the {@link Scanner} that was
 * passed in. Used mostly for testing. Does not implement any method other than {@link
 * ScannerSupplier#get()}.
 */
class InstanceReturningScannerSupplierImpl extends ScannerSupplier {
  private final Scanner scanner;

  InstanceReturningScannerSupplierImpl(Scanner scanner) {
    this.scanner = scanner;
  }

  @Override
  public Scanner get() {
    return scanner;
  }

  @Override
  public ImmutableBiMap<String, BugCheckerInfo> getAllChecks() {
    // TODO(cushon): migrate users off Scanner-based ScannerSuppliers, and throw UOE here
    return ImmutableBiMap.of();
  }

  @Override
  public ImmutableMap<String, SeverityLevel> severities() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ImmutableSet<String> disabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ErrorProneFlags getFlags() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableSet<BugCheckerInfo> getEnabledChecks() {
    throw new UnsupportedOperationException();
  }
}
