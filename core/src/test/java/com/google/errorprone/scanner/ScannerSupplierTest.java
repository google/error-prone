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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugCheckerSupplier.fromClass;
import static com.google.errorprone.BugCheckerSupplier.fromInstance;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.DepAnn;
import com.google.errorprone.bugpatterns.LongLiteralLowerCaseSuffix;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNull;
import com.google.errorprone.bugpatterns.StaticAccessedFromInstance;
import com.google.errorprone.bugpatterns.StringEquality;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

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

    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "ArrayEquals", fromInstance(new ArrayEquals()),
        "StaticAccessedFromInstance", fromInstance(new StaticAccessedFromInstance()));

    assertThat(ss.getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void fromBugCheckersWorks() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new StaticAccessedFromInstance());

    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "ArrayEquals", fromClass(ArrayEquals.class),
        "StaticAccessedFromInstance", fromClass(StaticAccessedFromInstance.class));

    assertThat(ss.getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void plusWorks() {
    ScannerSupplier ss1 = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new StaticAccessedFromInstance());
    ScannerSupplier ss2 = ScannerSupplier.fromBugCheckers(
        new BadShiftAmount(),
        new PreconditionsCheckNotNull());

    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "ArrayEquals", fromInstance(new ArrayEquals()),
        "StaticAccessedFromInstance", fromInstance(new StaticAccessedFromInstance()),
        "BadShiftAmount", fromInstance(new BadShiftAmount()),
        "PreconditionsCheckNotNull", fromInstance(new PreconditionsCheckNotNull()));

    assertThat(ss1.plus(ss2).getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void plusDoesntAllowDuplicateChecks() {
    ScannerSupplier ss1 = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(), new StaticAccessedFromInstance());
    ScannerSupplier ss2 = ScannerSupplier.fromBugCheckers(
        new ArrayEquals());

    try {
      ScannerSupplier result = ss1.plus(ss2);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).contains("ArrayEquals");
    }
  }

  @Test
  public void filterWorks() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new BadShiftAmount(),
        new StaticAccessedFromInstance());
    Predicate<BugCheckerSupplier> isBadShiftAmount = new Predicate<BugCheckerSupplier>() {
      @Override
      public boolean apply(BugCheckerSupplier input) {
        return input.canonicalName().equals("BadShiftAmount");
      }
    };

    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "BadShiftAmount", fromInstance(new BadShiftAmount()));

    assertThat(ss.filter(isBadShiftAmount).getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesWorksOnEmptySeverityMap() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ChainingConstructorIgnoresParameter(),
        new DepAnn(),
        new LongLiteralLowerCaseSuffix());
    Map<String, Severity> overrideMap = ImmutableMap.of();

    Map<String, BugCheckerSupplier> expected = ss.getNameToSupplierMap();
    assertThat(ss.applyOverrides(overrideMap).getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesEnablesCheck() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers();
    Map<String, Severity> overrideMap = ImmutableMap.of(
        "ArrayEquals", Severity.DEFAULT,
        "BadShiftAmount", Severity.DEFAULT);

    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "ArrayEquals", fromInstance(new ArrayEquals()),
        "BadShiftAmount", fromInstance(new BadShiftAmount()));

    assertThat(ss.applyOverrides(overrideMap).getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesDisablesChecks() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ChainingConstructorIgnoresParameter(),
        new DepAnn(),
        new LongLiteralLowerCaseSuffix());
    Map<String, Severity> overrideMap = ImmutableMap.of(
        "LongLiteralLowerCaseSuffix", Severity.OFF,
        "ChainingConstructorIgnoresParameter", Severity.OFF);

    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "DepAnn", fromInstance(new DepAnn()));

    assertThat(ss.applyOverrides(overrideMap).getNameToSupplierMap()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesThrowsExceptionWhenDisablingNonDisablableCheck() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(new ArrayEquals());
    Map<String, Severity> overrideMap = ImmutableMap.of("ArrayEquals", Severity.OFF);

    try {
      ScannerSupplier result = ss.applyOverrides(overrideMap);
      fail();
    } catch (InvalidCommandLineOptionException expected) {
      assertThat(expected.getMessage()).contains("may not be disabled");
    }
  }

  @Test
  public void applyOverridesSetsSeverity() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new BadShiftAmount(),
        new StringEquality());
    Map<String, Severity> overrideMap = ImmutableMap.of(
        "ArrayEquals", Severity.WARN,
        "StringEquality", Severity.ERROR);
    ScannerSupplier overriddenScannerSupplier = ss.applyOverrides(overrideMap);

    Map<String, BugCheckerSupplier> unexpected = ImmutableMap.of(
        "ArrayEquals", fromInstance(new ArrayEquals()),
        "BadShiftAmount", fromInstance(new BadShiftAmount()),
        "StringEquality", fromInstance(new StringEquality()));
    assertThat(overriddenScannerSupplier.getNameToSupplierMap()).isNotEqualTo(unexpected);

    BugChecker arrayEqualsWithWarningSeverity = new ArrayEquals();
    arrayEqualsWithWarningSeverity.setSeverity(SeverityLevel.WARNING);
    BugChecker stringEqualityWithErrorSeverity = new StringEquality();
    stringEqualityWithErrorSeverity.setSeverity(SeverityLevel.ERROR);
    Map<String, BugCheckerSupplier> expected = ImmutableMap.of(
        "ArrayEquals", fromInstance(arrayEqualsWithWarningSeverity),
        "BadShiftAmount", fromInstance(new BadShiftAmount()),
        "StringEquality", fromInstance(stringEqualityWithErrorSeverity));
    assertThat(overriddenScannerSupplier.getNameToSupplierMap()).isEqualTo(expected);
  }
}
