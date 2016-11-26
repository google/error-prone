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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getFirst;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import java.io.Serializable;

/**
 * An implementation of a {@link ScannerSupplier}, abstracted as a set of all known
 * {@link BugChecker}s and a set of enabled {@link BugChecker}s. The set of enabled suppliers must
 * be a subset of all known suppliers.
 */
class ScannerSupplierImpl extends ScannerSupplier implements Serializable {
  private final ImmutableBiMap<String, BugCheckerInfo> checks;
  private final ImmutableMap<String, BugPattern.SeverityLevel> severities;
  private final ImmutableSet<String> disabled;

  ScannerSupplierImpl(
      ImmutableBiMap<String, BugCheckerInfo> checks,
      ImmutableMap<String, BugPattern.SeverityLevel> severities,
      ImmutableSet<String> disabled) {
    checkArgument(
        Sets.difference(severities.keySet(), checks.keySet()).isEmpty(),
        "enabledChecks must be a subset of allChecks");
    checkArgument(
        Sets.difference(disabled, checks.keySet()).isEmpty(),
        "disabled must be a subset of allChecks");
    this.checks = checks;
    this.severities = severities;
    this.disabled = disabled;
  }

  private static final Function<BugCheckerInfo, BugChecker> INSTANTIATE_CHECKER =
      new Function<BugCheckerInfo, BugChecker>() {
        @Override
        public BugChecker apply(BugCheckerInfo checkerClass) {
          try {
            return checkerClass.checkerClass().getConstructor().newInstance();
          } catch (ReflectiveOperationException e) {
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
  public ImmutableMap<String, BugPattern.SeverityLevel> severities() {
    return severities;
  }

  @Override
  protected ImmutableSet<String> disabled() {
    return disabled;
  }

  @Override
  public ImmutableSet<BugCheckerInfo> getEnabledChecks() {
    return FluentIterable.from(getAllChecks().values())
        .filter(
            new Predicate<BugCheckerInfo>() {
              @Override
              public boolean apply(BugCheckerInfo input) {
                return !disabled.contains(input.canonicalName());
              }
            })
        .toSet();
  }

  /** Returns the name of the first check, or {@code null}. */
  @Override
  public String toString() {
    return getFirst(getAllChecks().keySet(), null);
  }
}
