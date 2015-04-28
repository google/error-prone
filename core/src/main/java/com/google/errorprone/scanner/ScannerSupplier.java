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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.BugChecker;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.CheckReturnValue;

/**
 * Supplies {@link Scanner}s and provides access to the backing sets of all {@link
 * BugChecker}s and enabled {@link BugChecker}s.
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

  private static PMap<String, BugPattern.SeverityLevel> defaultSeverities(
      Iterable<BugCheckerInfo> checkers) {
    PMap<String, BugPattern.SeverityLevel> severities = HashTreePMap.empty();
    for (BugCheckerInfo check : checkers) {
      severities = severities.plus(check.canonicalName(), check.defaultSeverity());
    }
    return severities;
  }

  /**
   * Returns a {@link ScannerSupplier} with a specific list of {@link BugChecker} classes.
   */
  public static ScannerSupplier fromBugCheckerClasses(
      Iterable<Class<? extends BugChecker>> checkers) {
    ImmutableList.Builder<BugCheckerInfo> builder = ImmutableList.builder();
    for (Class<? extends BugChecker> checker : checkers) {
      builder.add(BugCheckerInfo.create(checker));
    }
    return fromBugCheckerInfos(builder.build());
  }

  /**
   * Returns a {@link ScannerSupplier} built from a list of {@link BugCheckerInfo}s.
   */
  public static ScannerSupplier fromBugCheckerInfos(Iterable<BugCheckerInfo> checkers) {
    ImmutableBiMap.Builder<String, BugCheckerInfo> builder = ImmutableBiMap.builder();
    for (BugCheckerInfo checker : checkers) {
      builder.put(checker.canonicalName(), checker);
    }
    ImmutableBiMap<String, BugCheckerInfo> allChecks = builder.build();
    return new ScannerSupplierImpl(allChecks, defaultSeverities(allChecks.values()));
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
   * Returns a map of check name to {@link BugCheckerInfo} for all {@link BugCheckerInfo}s in this
   * {@link ScannerSupplier}, including disabled ones.
   */
  protected abstract ImmutableBiMap<String, BugCheckerInfo> getAllChecks();

  /**
   * Returns the set of {@link BugCheckerInfo}s that are enabled in this {@link ScannerSupplier}.
   */
  protected abstract ImmutableSet<BugCheckerInfo> getEnabledChecks();

  protected abstract PMap<String, BugPattern.SeverityLevel> severities();

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
    Map<String, Severity> severityOverrides = errorProneOptions.getSeverityMap();
    if (severityOverrides.isEmpty()) {
      return this;
    }

    // Initialize result allChecks map and enabledChecks set with current state of this Supplier.
    ImmutableBiMap<String, BugCheckerInfo> checks = getAllChecks();
    PMap<String, SeverityLevel> severities = severities();

    // Create a map from names (canonical and alternate) to checks. We could do this when the
    // supplier is created, but applyOverrides() is unlikely to be called more than once per
    // scanner instance.
    Multimap<String, BugCheckerInfo> checksByAllNames = ArrayListMultimap.create();
    for (BugCheckerInfo checker : getAllChecks().values()) {
      for (String name : checker.allNames()) {
        checksByAllNames.put(name, checker);
      }
    }
    
    // Process overrides
    for (Entry<String, Severity> entry : severityOverrides.entrySet()) {
      Collection<BugCheckerInfo> checksWithName = checksByAllNames.get(entry.getKey());
      if (checksWithName.isEmpty()) {
        if (errorProneOptions.ignoreUnknownChecks()) {
          continue;
        }
        throw new InvalidCommandLineOptionException(
            entry.getKey() + " is not a valid checker name");
      }
      for (BugCheckerInfo check : checksWithName) {
        switch (entry.getValue()) {
          case OFF:
            if (!check.suppressibility().disableable()) {
              throw new InvalidCommandLineOptionException(
                  check.canonicalName() + " may not be disabled");
            }
            severities = severities.plus(check.canonicalName(), SeverityLevel.NOT_A_PROBLEM);
            break;
          case DEFAULT:
            severities = severities.plus(check.canonicalName(), check.defaultSeverity());
            break;
          case WARN:
            // Demoting an enabled check from an error to a warning is a form of disabling
            if (check.severity(severities).enabled()
                && !check.suppressibility().disableable()
                && check.defaultSeverity() == SeverityLevel.ERROR) {
              throw new InvalidCommandLineOptionException(check.canonicalName()
                  + " is not disableable and may not be demoted to a warning");
            }
            severities = severities.plus(check.canonicalName(), SeverityLevel.WARNING);
            break;
          case ERROR:
            severities = severities.plus(check.canonicalName(), SeverityLevel.ERROR);
            break;
          default:
            throw new IllegalStateException("Unexpected severity level: " + entry.getValue());
        }
      }
    }

    return new ScannerSupplierImpl(checks, severities);
  }

  /**
   * Composes this {@link ScannerSupplier} with the {@code other} {@link ScannerSupplier}. The set
   * of checks that are turned on is the union of the set of checks on in {@code this} and
   * {@code other}.
   */
  @CheckReturnValue
  public ScannerSupplier plus(ScannerSupplier other) {
    ImmutableBiMap<String, BugCheckerInfo> combinedAllChecks =
        ImmutableBiMap.<String, BugCheckerInfo>builder()
            .putAll(this.getAllChecks())
            .putAll(other.getAllChecks())
            .build();
    PMap<String, SeverityLevel> combinedSeverities =
        this.severities().plusAll(other.severities());
    return new ScannerSupplierImpl(combinedAllChecks, combinedSeverities);
  }

  /**
   * Filters this {@link ScannerSupplier} based on the provided predicate. Returns a
   * {@link ScannerSupplier} with only the checks enabled that satisfy the predicate.
   */
  @CheckReturnValue
  public ScannerSupplier filter(Predicate<? super BugCheckerInfo> predicate) {
    PMap<String, SeverityLevel> filteredSeverities = severities();
    for (Entry<String, SeverityLevel> entry : severities().entrySet()) {
      if (!predicate.apply(getAllChecks().get(entry.getKey()))) {
        filteredSeverities = filteredSeverities.plus(entry.getKey(), SeverityLevel.NOT_A_PROBLEM);
      }
    }
    return new ScannerSupplierImpl(getAllChecks(), filteredSeverities);
  }
}
