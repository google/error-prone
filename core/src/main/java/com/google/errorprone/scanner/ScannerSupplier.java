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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.BugChecker;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

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
    ImmutableBiMap.Builder<String, BugCheckerSupplier> builder = ImmutableBiMap.builder();
    for (Class<? extends BugChecker> checkerClass : checkerClasses) {
      BugCheckerSupplier supplier = CLASS_TO_SUPPLIER.apply(checkerClass);
      builder.put(supplier.canonicalName(), supplier);
    }
    return new ScannerSupplierImpl(builder.build());
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
      BugCheckerSupplier supplier = INSTANCE_TO_SUPPLIER.apply(checker);
      builder.put(supplier.canonicalName(), supplier);
    }
    return new ScannerSupplierImpl(builder.build());
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
   * Returns a map of check name to {@link BugCheckerSupplier} for all {@link BugCheckerSupplier}s
   * in this {@link ScannerSupplier}.
   */
  abstract ImmutableBiMap<String, BugCheckerSupplier> getNameToSupplierMap();

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

    // Initialize result map with current state of this Supplier.  We use a mutable BiMap here
    // rather than ImmutableBiMap.Builder so that (1) we can replace existing mappings with new
    // ones to override the severity of a check, and (2) we can remove checks that are overridden
    // to off.  For (1), BiMap.put() replaces the existing mapping for a key if it exists, whereas
    // ImmutableBiMap.Builder would throw an exception on build().
    BiMap<String, BugCheckerSupplier> result = HashBiMap.create(getNameToSupplierMap());

    // Process overrides
    for (Entry<String, Severity> entry : severityMap.entrySet()) {
      BugCheckerSupplier supplier = forName(entry.getKey());
      if (supplier == null) {
        throw new InvalidCommandLineOptionException(
            entry.getKey() + " is not a valid checker name");
      }
      switch (entry.getValue()) {
        case OFF:
          if (!supplier.disableable()) {
            throw new InvalidCommandLineOptionException(
                supplier.canonicalName() + " may not be disabled");
          }
          result.remove(entry.getKey());
          break;
        case DEFAULT:
          result.put(supplier.canonicalName(), supplier);
          break;
        case WARN:
          supplier = supplier.overrideSeverity(SeverityLevel.WARNING);
          result.put(supplier.canonicalName(), supplier);
          break;
        case ERROR:
          supplier = supplier.overrideSeverity(SeverityLevel.ERROR);
          result.put(supplier.canonicalName(), supplier);
          break;
        default:
          throw new IllegalStateException("Unexpected severity level: " + entry.getValue());
      }
    }

    return new ScannerSupplierImpl(ImmutableBiMap.copyOf(result));
  }

  /**
   * Composes this {@link ScannerSupplier} with the {@code other} {@link ScannerSupplier}.  The
   * set of checks that are turned on is the union of the set of checks on in {@code this} and
   * {@code other}.
   */
  @CheckReturnValue
  public ScannerSupplier plus(ScannerSupplier other) {
    ImmutableBiMap<String, BugCheckerSupplier> result =
        ImmutableBiMap.<String, BugCheckerSupplier>builder()
            .putAll(this.getNameToSupplierMap())
            .putAll(other.getNameToSupplierMap())
            .build();
    return new ScannerSupplierImpl(result);
  }

  /**
   * Filters this {@link ScannerSupplier} based on the provided predicate.  Returns a
   * {@link ScannerSupplier} with only the checks that satisfy the predicate.
   */
  @CheckReturnValue
  public ScannerSupplier filter(Predicate<? super BugCheckerSupplier> predicate) {
    ImmutableBiMap.Builder<String, BugCheckerSupplier> builder = ImmutableBiMap.builder();
    for (Entry<String, BugCheckerSupplier> entry : this.getNameToSupplierMap().entrySet()) {
      if (predicate.apply(entry.getValue())) {
        builder.put(entry);
      }
    }
    return new ScannerSupplierImpl(builder.build());
  }

  /**
   * Returns the {@link BugCheckerSupplier} that corresponds to the given {@code name}.  Returns
   * null if no checker is found with that name.
   *
   * <p>First searches the {@link BugCheckerSupplier}s that are part of this
   * {@link ScannerSupplier}, then searches error-prone's built-in {@link BugChecker}s.  Note that
   * this does not search any plugin checkers unless this {@link ScannerSupplier} includes those
   * checkers (e.g., by calling {@link ScannerSupplier#fromBugCheckers} with a list of the
   * {@link BugChecker}s from the plugin path).
   */
  private BugCheckerSupplier forName(String name) {
    BugCheckerSupplier result = getNameToSupplierMap().get(name);
    if (result != null) {
      return result;
    }
    return BuiltInCheckerSuppliers.forName(name);
  }
}
