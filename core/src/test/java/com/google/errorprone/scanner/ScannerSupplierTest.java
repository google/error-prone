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
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneJavaCompilerTest;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.LongLiteralLowerCaseSuffix;
import com.google.errorprone.bugpatterns.Overrides;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNull;
import com.google.errorprone.bugpatterns.StaticAccessedFromInstance;
import com.google.errorprone.bugpatterns.StringEquality;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.scanner.BuiltInCheckerSuppliers.getSuppliers;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ScannerSupplier}.
 */
@RunWith(JUnit4.class)
public class ScannerSupplierTest {
  @Test
  public void fromBugCheckerClassesWorks() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckerClasses(
        ArrayEquals.class,
        StaticAccessedFromInstance.class);

    Set<BugCheckerInfo> expected =
        getSuppliers(ArrayEquals.class, StaticAccessedFromInstance.class);

    assertThat(ss.getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void fromBugCheckersWorks() {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class, StaticAccessedFromInstance.class);

    Set<BugCheckerInfo> expected =
        getSuppliers(ArrayEquals.class, StaticAccessedFromInstance.class);

    assertThat(ss.getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void plusWorks() {
    ScannerSupplier ss1 =
        ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class, StaticAccessedFromInstance.class);
    ScannerSupplier ss2 =
        ScannerSupplier.fromBugCheckerClasses(
            BadShiftAmount.class, PreconditionsCheckNotNull.class);

    Set<BugCheckerInfo> expected =
        getSuppliers(
            ArrayEquals.class,
            StaticAccessedFromInstance.class,
            BadShiftAmount.class,
            PreconditionsCheckNotNull.class);

    assertThat(ss1.plus(ss2).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  // Calling ScannerSupplier.plus() just to make sure it throws the right exception
  @SuppressWarnings("CheckReturnValue")
  public void plusDoesntAllowDuplicateChecks() {
    ScannerSupplier ss1 =
        ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class, StaticAccessedFromInstance.class);
    ScannerSupplier ss2 = ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class);

    try {
      ss1.plus(ss2);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).contains("ArrayEquals");
    }
  }

  @Test
  public void filterWorks() {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ArrayEquals.class, BadShiftAmount.class, StaticAccessedFromInstance.class);
    Predicate<BugCheckerInfo> isBadShiftAmount =
        new Predicate<BugCheckerInfo>() {
      @Override
          public boolean apply(BugCheckerInfo input) {
        return input.canonicalName().equals("BadShiftAmount");
      }
    };

    Set<BugCheckerInfo> expected = getSuppliers(BadShiftAmount.class);

    assertThat(ss.filter(isBadShiftAmount).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesWorksOnEmptySeverityMap() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ChainingConstructorIgnoresParameter.class,
            Overrides.class,
            LongLiteralLowerCaseSuffix.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(Collections.<String>emptyList());

    Set<BugCheckerInfo> expected = ss.getEnabledChecks();
    assertThat(ss.applyOverrides(epOptions).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesEnablesCheck() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier
            .fromBugCheckerClasses(
                ArrayEquals.class, BadShiftAmount.class, StaticAccessedFromInstance.class)
        .filter(Predicates.alwaysFalse());    // disables all checks

    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-Xep:ArrayEquals", "-Xep:BadShiftAmount"));

    Set<BugCheckerInfo> expected = getSuppliers(ArrayEquals.class, BadShiftAmount.class);

    assertThat(ss.applyOverrides(epOptions).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesDisablesChecks() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ChainingConstructorIgnoresParameter.class,
            Overrides.class,
            LongLiteralLowerCaseSuffix.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of(
            "-Xep:LongLiteralLowerCaseSuffix:OFF",
            "-Xep:ChainingConstructorIgnoresParameter:OFF"));

    Set<BugCheckerInfo> expected = getSuppliers(Overrides.class);

    assertThat(ss.applyOverrides(epOptions).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  // Calling ScannerSupplier.applyOverrides() just to make sure it throws the right exception
  @SuppressWarnings("CheckReturnValue")
  public void applyOverridesThrowsExceptionWhenDisablingNonDisablableCheck() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-Xep:ArrayEquals:OFF"));

    try {
      ss.applyOverrides(epOptions);
      fail();
    } catch (InvalidCommandLineOptionException expected) {
      assertThat(expected.getMessage()).contains("may not be disabled");
    }
  }

  @Test
  // Calling ScannerSupplier.applyOverrides() just to make sure it throws the right exception
  @SuppressWarnings("CheckReturnValue")
  public void applyOverridesThrowsExceptionWhenDemotingNonDisablableCheck() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-Xep:ArrayEquals:WARN"));

    try {
      ss.applyOverrides(epOptions);
      fail();
    } catch (InvalidCommandLineOptionException expected) {
      assertThat(expected.getMessage()).contains("may not be demoted to a warning");
    }
  }

  @Test
  // Calling ScannerSupplier.applyOverrides() just to make sure it does not throw an exception
  @SuppressWarnings("CheckReturnValue")
  public void applyOverridesSucceedsWhenDisablingUnknownCheckAndIgnoreUnknownCheckNamesIsSet()
      throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-XepIgnoreUnknownCheckNames", "-Xep:foo:OFF"));

    ss.applyOverrides(epOptions);
  }

  @Test
  public void applyOverridesSetsSeverity() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            BadShiftAmount.class, ChainingConstructorIgnoresParameter.class, StringEquality.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of(
            "-Xep:ChainingConstructorIgnoresParameter:WARN",
            "-Xep:StringEquality:ERROR"));
    ScannerSupplier overriddenScannerSupplier = ss.applyOverrides(epOptions);
    
    Map<String, SeverityLevel> expected = ImmutableMap.of(
        "BadShiftAmount", SeverityLevel.ERROR,
        "ChainingConstructorIgnoresParameter", SeverityLevel.WARNING,
        "StringEquality", SeverityLevel.ERROR);

    assertThat(overriddenScannerSupplier.severities()).isEqualTo(expected);
  }
}
