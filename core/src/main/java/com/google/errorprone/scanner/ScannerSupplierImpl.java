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
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;

import org.pcollections.PMap;

/**
 * An implementation of a {@link ScannerSupplier}, abstracted as a set of all known
 * {@link BugChecker}s and a set of enabled {@link BugChecker}s. The set of enabled suppliers must
 * be a subset of all known suppliers.
 */
class ScannerSupplierImpl extends ScannerSupplier {
  private final ImmutableBiMap<String, BugCheckerInfo> checks;
  private final PMap<String, BugPattern.SeverityLevel> severities;

  ScannerSupplierImpl(
      ImmutableBiMap<String, BugCheckerInfo> checks,
      PMap<String, BugPattern.SeverityLevel> severities) {
    Preconditions.checkArgument(
        Sets.difference(severities.keySet(), checks.keySet()).isEmpty(),
        "enabledChecks must be a subset of allChecks");
    this.checks = checks;
    this.severities = severities;
  }

  // TODO(cushon): BugCheckerSupplier::get
  private static final Function<BugCheckerInfo, BugChecker> INSTANTIATE_CHECKER =
      new Function<BugCheckerInfo, BugChecker>() {
        @Override
        public BugChecker apply(BugCheckerInfo checkerClass) {
          try {
            return checkerClass.checkerClass().newInstance();
          } catch (InstantiationException | IllegalAccessException e) {
            throw new LinkageError("Could not instantiate BugChecker.", e);
          }
        }
      };

  @Override
  public ErrorProneScanner get() {
    return new ErrorProneScanner(
        Iterables.transform(getEnabledChecks(), INSTANTIATE_CHECKER), severities);
  }

  @Override
  public ImmutableBiMap<String, BugCheckerInfo> getAllChecks() {
    return checks;
  }

  @Override
  protected PMap<String, BugPattern.SeverityLevel> severities() {
    return severities;
  }

  @Override
  public ImmutableSet<BugCheckerInfo> getEnabledChecks() {
    return FluentIterable.from(getAllChecks().values()).filter(isCheckEnabled).toSet();
  }

  private final Predicate<BugCheckerInfo> isCheckEnabled =
      new Predicate<BugCheckerInfo>() {
        @Override
        public boolean apply(BugCheckerInfo input) {
          return input.severity(severities).enabled();
        }
  };
}
