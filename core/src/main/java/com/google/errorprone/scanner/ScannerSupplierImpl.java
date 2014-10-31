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
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.bugpatterns.BugChecker;

/**
 * An implementation of a {@link ScannerSupplier}, abstracted as a set of
 * {@link BugCheckerSupplier}s.
 */
class ScannerSupplierImpl extends ScannerSupplier {
  private final ImmutableSet<BugCheckerSupplier> suppliers;

  ScannerSupplierImpl(ImmutableSet<BugCheckerSupplier> suppliers) {
    this.suppliers = suppliers;
  }

  @Override
  public ImmutableSet<BugCheckerSupplier> getSuppliers() {
    return suppliers;
  }

  @Override
  public Scanner get() {
    Iterable<BugChecker> checkers = FluentIterable.from(suppliers).transform(SUPPLIER_GET);
    return new ErrorProneScanner(checkers);
  }

  private static final Function<Supplier<BugChecker>, BugChecker> SUPPLIER_GET =
      new Function<Supplier<BugChecker>, BugChecker>() {
    @Override
    public BugChecker apply(Supplier<BugChecker> input) {
      return input.get();
    }
  };
}
