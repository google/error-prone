/*
 * Copyright 2014 The Error Prone Authors.
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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.bugpatterns.BugChecker;
import java.io.Serializable;

/**
 * An implementation of a {@link ScannerSupplier}, abstracted as a set of all known {@link
 * BugChecker}s and a set of enabled {@link BugChecker}s. The set of enabled suppliers must be a
 * subset of all known suppliers.
 */
class ScannerSupplierImpl extends ScannerSupplier implements Serializable {
  private final ImmutableBiMap<String, BugCheckerInfo> checks;
  private final ImmutableMap<String, SeverityLevel> severities;
  private final ImmutableSet<String> disabled;
  private final ErrorProneFlags flags;
  // Lazily initialized to make serialization easy.
  private transient ErrorProneInjector injector;

  ScannerSupplierImpl(
      ImmutableBiMap<String, BugCheckerInfo> checks,
      ImmutableMap<String, SeverityLevel> severities,
      ImmutableSet<String> disabled,
      ErrorProneFlags flags) {
    checkArgument(
        Sets.difference(severities.keySet(), checks.keySet()).isEmpty(),
        "enabledChecks must be a subset of allChecks");
    checkArgument(
        Sets.difference(disabled, checks.keySet()).isEmpty(),
        "disabled must be a subset of allChecks");
    this.checks = checks;
    this.severities = severities;
    this.disabled = disabled;
    this.flags = flags;
  }

  private BugChecker instantiateChecker(BugCheckerInfo checker) {
    if (injector == null) {
      injector = ErrorProneInjector.create().addBinding(ErrorProneFlags.class, flags);
    }
    return injector.getInstance(checker.checkerClass());
  }

  @Override
  public ErrorProneScanner get() {
    return new ErrorProneScanner(
        getEnabledChecks().stream()
            .map(this::instantiateChecker)
            .collect(ImmutableSet.toImmutableSet()),
        severities);
  }

  @Override
  public ImmutableBiMap<String, BugCheckerInfo> getAllChecks() {
    return checks;
  }

  @Override
  public ImmutableMap<String, SeverityLevel> severities() {
    return severities;
  }

  @Override
  protected ImmutableSet<String> disabled() {
    return disabled;
  }

  @Override
  public ImmutableSet<BugCheckerInfo> getEnabledChecks() {
    return getAllChecks().values().stream()
        .filter(input -> !disabled.contains(input.canonicalName()))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ErrorProneFlags getFlags() {
    return flags;
  }

  /** Returns the name of the first check, or {@code ""}. */
  @Override
  public String toString() {
    return getFirst(getAllChecks().keySet(), "");
  }
}
