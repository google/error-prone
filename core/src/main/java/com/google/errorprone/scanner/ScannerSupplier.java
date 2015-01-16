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

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions;
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
 * Supplies {@link Scanner}s and provides access to the backing sets of all {@link
 * BugCheckerSupplier}s and enabled {@link BugCheckerSupplier}s.
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
    ImmutableBiMap.Builder<String, BugCheckerSupplier> builder = ImmutableBiMap.builder();
    for (Class<? extends BugChecker> checkerClass : checkerClasses) {
      BugCheckerSupplier supplier = BugCheckerSupplier.fromClass(checkerClass);
      builder.put(supplier.canonicalName(), supplier);
    }
    ImmutableBiMap<String, BugCheckerSupplier> allChecks = builder.build();
    return new ScannerSupplierImpl(allChecks, allChecks.values());
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
    ImmutableBiMap.Builder<String, BugCheckerSupplier> builder = ImmutableBiMap.builder();
    for (BugChecker checker : checkers) {
      BugCheckerSupplier supplier = BugCheckerSupplier.fromInstance(checker);
      builder.put(supplier.canonicalName(), supplier);
    }
    ImmutableBiMap<String, BugCheckerSupplier> allChecks = builder.build();
    return new ScannerSupplierImpl(allChecks, allChecks.values());
  }

  /**
   * Returns a {@link ScannerSupplier} that just returns the {@link Scanner} that was passed in.
   * Used mostly for testing.  Does not implement any method other than
   * {@link ScannerSupplier#get()}.
   */
  public static ScannerSupplier fromScanner(Scanner scanner) {
    return new InstanceReturningScannerSupplierImpl(scanner);
  }


  /* Instance methods */

  /**
   * Returns a map of check name to {@link BugCheckerSupplier} for all {@link BugCheckerSupplier}s
   * in this {@link ScannerSupplier}, including disabled ones.
   */
  protected abstract ImmutableBiMap<String, BugCheckerSupplier> getAllChecks();

  /**
   * Returns the set of {@link BugCheckerSupplier}s that are enabled in this {@link
   * ScannerSupplier}.
   */
  protected abstract ImmutableSet<BugCheckerSupplier> getEnabledChecks();

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
   * @param errorProneOptions an {@link ErrorProneOptions} object that encapsulates the overrides
   * for this compilation
   * @throws InvalidCommandLineOptionException if the override map attempts to disable a check
   * that may not be disabled
   */
  @CheckReturnValue
  public ScannerSupplier applyOverrides(ErrorProneOptions errorProneOptions)
      throws InvalidCommandLineOptionException {
    Map<String, Severity> severityMap = errorProneOptions.getSeverityMap();
    if (severityMap.isEmpty()) {
      return this;
    }

    // Initialize result allChecks map and enabledChecks set with current state of this Supplier.
    // We use mutable data structures here so that (1) we can replace existing BugCheckerSuppliers
    // with new ones to override the severity of a check, and (2) we can remove checks from
    // enabledChecks if they are overridden to off.
    BiMap<String, BugCheckerSupplier> allChecks = HashBiMap.create(getAllChecks());
    Set<BugCheckerSupplier> enabledChecks = new HashSet<>(getEnabledChecks());

    // Process overrides
    for (Entry<String, Severity> entry : severityMap.entrySet()) {
      BugCheckerSupplier supplier = forName(entry.getKey());
      BugCheckerSupplier newSupplier;
      if (supplier == null) {
        if (errorProneOptions.ignoreUnknownChecks()) {
          continue;
        }
        throw new InvalidCommandLineOptionException(
            entry.getKey() + " is not a valid checker name");
      }
      switch (entry.getValue()) {
        case OFF:
          if (!supplier.suppressibility().disableable()) {
            throw new InvalidCommandLineOptionException(
                supplier.canonicalName() + " may not be disabled");
          }
          enabledChecks.remove(supplier);
          break;
        case DEFAULT:
          enabledChecks.add(supplier);
          break;
        case WARN:
          // Demoting an enabled check from an error to a warning is a form of disabling
          if (enabledChecks.contains(supplier)
              && !supplier.suppressibility().disableable()
              && supplier.severity() == SeverityLevel.ERROR) {
            throw new InvalidCommandLineOptionException(supplier.canonicalName()
                + " is not disableable and may not be demoted to a warning");
          }

          // When the severity of a check is overridden, a new BugCheckerSupplier is produced.
          // The old BugCheckerSupplier must be removed from allChecks and enabledChecks,
          // and the new BugCheckerSupplier must be added.
          newSupplier = supplier.overrideSeverity(SeverityLevel.WARNING);
          enabledChecks.remove(supplier);
          allChecks.put(newSupplier.canonicalName(), newSupplier);
          enabledChecks.add(newSupplier);
          break;
        case ERROR:
          // When the severity of a check is overridden, a new BugCheckerSupplier is produced.
          // The old BugCheckerSupplier must be removed from allChecks and enabledChecks,
          // and the new BugCheckerSupplier must be added.
          newSupplier = supplier.overrideSeverity(SeverityLevel.ERROR);
          enabledChecks.remove(supplier);
          allChecks.put(newSupplier.canonicalName(), newSupplier);
          enabledChecks.add(newSupplier);
          break;
        default:
          throw new IllegalStateException("Unexpected severity level: " + entry.getValue());
      }
    }

    return new ScannerSupplierImpl(ImmutableBiMap.copyOf(allChecks),
        ImmutableSet.copyOf(enabledChecks));
  }

  /**
   * Composes this {@link ScannerSupplier} with the {@code other} {@link ScannerSupplier}.  The
   * set of checks that are turned on is the union of the set of checks on in {@code this} and
   * {@code other}.
   */
  @CheckReturnValue
  public ScannerSupplier plus(ScannerSupplier other) {
    ImmutableBiMap<String, BugCheckerSupplier> combinedAllChecks =
        ImmutableBiMap.<String, BugCheckerSupplier>builder()
            .putAll(this.getAllChecks())
            .putAll(other.getAllChecks())
            .build();
    ImmutableSet<BugCheckerSupplier> combinedEnabledChecks =
        ImmutableSet.<BugCheckerSupplier>builder()
            .addAll(this.getEnabledChecks())
            .addAll(other.getEnabledChecks())
            .build();
    return new ScannerSupplierImpl(combinedAllChecks, combinedEnabledChecks);
  }

  /**
   * Filters this {@link ScannerSupplier} based on the provided predicate.  Returns a
   * {@link ScannerSupplier} with only the checks enabled that satisfy the predicate.
   */
  @CheckReturnValue
  public ScannerSupplier filter(Predicate<? super BugCheckerSupplier> predicate) {
    return new ScannerSupplierImpl(getAllChecks(),
        FluentIterable.from(getEnabledChecks()).filter(predicate).toSet());
  }

  /**
   * Searches the checkers in this {@link ScannerSupplier} and returns the {@link
   * BugCheckerSupplier} with the given {@code name}.  Returns null if no checker is found with
   * that name.
   */
  private BugCheckerSupplier forName(String name) {
    return getAllChecks().get(name);
  }
}
