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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.bugpatterns.BugChecker;

/**
 * An implementation of a {@link ScannerSupplier}, abstracted as a set of all known
 * {@link BugCheckerSupplier}s and a set of enabled {@link BugCheckerSupplier}s.  The set of
 * enabled suppliers must be a subset of all known suppliers.
 */
class ScannerSupplierImpl extends ScannerSupplier {
  private final ImmutableBiMap<String, BugCheckerSupplier> allChecks;
  private final ImmutableSet<BugCheckerSupplier> enabledChecks;

  ScannerSupplierImpl(ImmutableBiMap<String, BugCheckerSupplier> allChecks,
      ImmutableSet<BugCheckerSupplier> enabledChecks) {
    Preconditions.checkArgument(
        Sets.intersection(allChecks.values(), enabledChecks).equals(enabledChecks),
        "enabledChecks must be a subset of allChecks");
    this.allChecks = allChecks;
    this.enabledChecks = enabledChecks;
  }

  @Override
  public Scanner get() {
    Iterable<BugChecker> checkers = FluentIterable
        .from(enabledChecks)
        .transform(SUPPLIER_GET);
    return new ErrorProneScanner(checkers);
  }

  @Override
  protected ImmutableBiMap<String, BugCheckerSupplier> getAllChecks() {
    return allChecks;
  }

  @Override
  protected ImmutableSet<BugCheckerSupplier> getEnabledChecks() {
    return enabledChecks;
  }

  private static final Function<Supplier<BugChecker>, BugChecker> SUPPLIER_GET =
      new Function<Supplier<BugChecker>, BugChecker>() {
    @Override
    public BugChecker apply(Supplier<BugChecker> input) {
      return input.get();
    }
  };
}
