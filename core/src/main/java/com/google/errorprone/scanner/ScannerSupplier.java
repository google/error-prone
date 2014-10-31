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
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.BugChecker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckReturnValue;

/**
 * Supplies {@link Scanner}s and provides access to the backing list of {@link BugCheckerSupplier}s.
 */
public abstract class ScannerSupplier implements Supplier<Scanner> {

  /* Static factory methods and helpers */

  /**
   * Returns a {@link ScannerSupplier} with a specific list of {@link BugChecker} classes.
   */
  @SafeVarargs
  public static ScannerSupplier fromBugCheckerClasses(
      Class<? extends BugChecker>... checkerClasses) {
    return fromBugCheckerClasses(Arrays.asList(checkerClasses));
  }

  /**
   * Returns a {@link ScannerSupplier} with a specific list of {@link BugChecker} classes.
   */
  public static ScannerSupplier fromBugCheckerClasses(
      Iterable<Class<? extends BugChecker>> checkerClasses) {
    ImmutableSet<BugCheckerSupplier> result = FluentIterable
        .from(checkerClasses)
        .transform(CLASS_TO_SUPPLIER)
        .toSet();
    return new ScannerSupplierImpl(result);
  }

  /**
   * Returns a {@link ScannerSupplier} built from a list of {@link BugChecker} instances.
   */
  public static ScannerSupplier fromBugCheckers(BugChecker... checkers) {
    return fromBugCheckers(Arrays.asList(checkers));
  }

  /**
   * Returns a {@link ScannerSupplier} built from a list of {@link BugChecker} instances.
   */
  public static ScannerSupplier fromBugCheckers(Iterable<? extends BugChecker> checkers) {
    ImmutableSet<BugCheckerSupplier> result = FluentIterable
        .from(checkers)
        .transform(INSTANCE_TO_SUPPLIER)
        .toSet();
    return new ScannerSupplierImpl(result);
  }

  /**
   * Returns a {@link ScannerSupplier} that just returns the {@link Scanner} that was passed in.
   * Used mostly for testing.  Does not implement any method other than
   * {@link ScannerSupplier#get()}.
   */
  public static ScannerSupplier fromScanner(Scanner scanner) {
    return new InstanceReturningScannerSupplierImpl(scanner);
  }

  /**
   * Transforms {@link BugChecker} instances into {@link BugCheckerSupplier}s.
   */
  private static final Function<BugChecker, BugCheckerSupplier> INSTANCE_TO_SUPPLIER =
      new Function<BugChecker, BugCheckerSupplier>() {
        @Override
        public BugCheckerSupplier apply(BugChecker input) {
          return BugCheckerSupplier.fromInstance(input);
        }
      };

  /**
   * Transforms {@link BugChecker} classes into {@link BugCheckerSupplier}s.
   */
  private static final Function<Class<? extends BugChecker>, BugCheckerSupplier> CLASS_TO_SUPPLIER =
      new Function<Class<? extends BugChecker>, BugCheckerSupplier>() {
        @Override
        public BugCheckerSupplier apply(Class<? extends BugChecker> input) {
          return BugCheckerSupplier.fromClass(input);
        }
      };


  /* Instance methods */

  /**
   * Returns all {@link BugCheckerSupplier}s in this {@link ScannerSupplier}
   */
  public abstract ImmutableSet<BugCheckerSupplier> getSuppliers();

  /**
   * Applies an override map (from command-line options) to this {@link ScannerSupplier} and
   * returns the resulting {@link ScannerSupplier}.  The overrides may do any of the following:
   * <ul>
   * <li>Enable a check that is currently off</li>
   * <li>Disable a check that is currently on</li>
   * <li>Change the severity of a check that is on, promoting a warning to an error or demoting
   * an error to a warning</li>
   * </ul>
   *
   * @param severityMap a map of check canonical names to their overridden severities
   * @throws InvalidCommandLineOptionException if the override map attempts to disable a check
   * that may not be disabled
   */
  @CheckReturnValue
  public ScannerSupplier applyOverrides(Map<String, Severity> severityMap)
      throws InvalidCommandLineOptionException {
    if (severityMap.isEmpty()) {
      return this;
    }

    // Process override map to collect names of checks that should be disabled.
    Set<String> disabledChecks = new HashSet<>();
    for (Entry<String, Severity> entry : severityMap.entrySet()) {
      if (entry.getValue() == Severity.OFF) {
        disabledChecks.add(entry.getKey());
      } else {
        // TODO(user): Support DEFAULT, WARN, and ERROR
        throw new UnsupportedOperationException(
            "Override to " + entry.getValue() + " not yet supported");
      }
    }

    // Filter list of BugCheckerSuppliers for those that should be disabled
    ImmutableSet.Builder<BugCheckerSupplier> resultSuppliers = ImmutableSet.builder();
    for (BugCheckerSupplier checkerSupplier : this.getSuppliers()) {
      boolean shouldDisable = disabledChecks.contains(checkerSupplier.canonicalName());
      if (shouldDisable && !checkerSupplier.disableable()) {
        throw new InvalidCommandLineOptionException(
            "error-prone check " + checkerSupplier.canonicalName() + " may not be disabled");
      }
      if (!shouldDisable) {
        resultSuppliers.add(checkerSupplier);
      }
    }
    return new ScannerSupplierImpl(resultSuppliers.build());
  }

  /**
   * Composes this {@link ScannerSupplier} with the {@code other} {@link ScannerSupplier}.  The
   * set of checks that are turned on is the union of the set of checks on in {@code this} and
   * {@code other}.
   */
  @CheckReturnValue
  public ScannerSupplier plus(ScannerSupplier other) {
    ImmutableSet<BugCheckerSupplier> result = FluentIterable
        .from(this.getSuppliers())
        .append(other.getSuppliers())
        .toSet();
    return new ScannerSupplierImpl(result);
  }

  /**
   * Filters this {@link ScannerSupplier} based on the provided predicate.  Returns a
   * {@link ScannerSupplier} with only the checks that satisfy the predicate.
   */
  @CheckReturnValue
  public ScannerSupplier filter(Predicate<? super BugCheckerSupplier> predicate) {
    ImmutableSet<BugCheckerSupplier> result = FluentIterable
        .from(this.getSuppliers())
        .filter(predicate)
        .toSet();
    return new ScannerSupplierImpl(result);
  }
}
