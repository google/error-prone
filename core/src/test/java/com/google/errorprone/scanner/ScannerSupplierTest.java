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

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.scanner.BuiltInCheckerSuppliers.getSuppliers;
import static org.junit.Assert.expectThrows;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneJavaCompilerTest;
import com.google.errorprone.ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.DepAnn;
import com.google.errorprone.bugpatterns.DivZero;
import com.google.errorprone.bugpatterns.EqualsIncompatibleType;
import com.google.errorprone.bugpatterns.LongLiteralLowerCaseSuffix;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNull;
import com.google.errorprone.bugpatterns.RestrictedApiChecker;
import com.google.errorprone.bugpatterns.StaticQualifiedUsingExpression;
import com.google.errorprone.bugpatterns.StringEquality;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ScannerSupplier}. */
@RunWith(JUnit4.class)
public class ScannerSupplierTest {

  @Test
  public void fromBugCheckerClassesWorks() {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ArrayEquals.class, StaticQualifiedUsingExpression.class);

    assertScanner(ss).hasEnabledChecks(ArrayEquals.class, StaticQualifiedUsingExpression.class);
  }

  @Test
  public void fromBugCheckersWorks() {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerInfos(
            ImmutableList.of(
                BugCheckerInfo.create(ArrayEquals.class),
                BugCheckerInfo.create(StaticQualifiedUsingExpression.class)));

    assertScanner(ss).hasEnabledChecks(ArrayEquals.class, StaticQualifiedUsingExpression.class);
  }

  @Test
  public void plusWorks() {
    ScannerSupplier ss1 =
        ScannerSupplier.fromBugCheckerClasses(
            ArrayEquals.class, StaticQualifiedUsingExpression.class);
    ScannerSupplier ss2 =
        ScannerSupplier.fromBugCheckerClasses(
            BadShiftAmount.class, PreconditionsCheckNotNull.class);

    assertScanner(ss1.plus(ss2))
        .hasEnabledChecks(
            ArrayEquals.class,
            StaticQualifiedUsingExpression.class,
            BadShiftAmount.class,
            PreconditionsCheckNotNull.class);
  }

  @Test
  public void plusDoesntAllowDuplicateChecks() {
    ScannerSupplier ss1 =
        ScannerSupplier.fromBugCheckerClasses(
            ArrayEquals.class, StaticQualifiedUsingExpression.class);
    ScannerSupplier ss2 = ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class);

    IllegalArgumentException expected =
        expectThrows(IllegalArgumentException.class, () -> ss1.plus(ss2));
    assertThat(expected.getMessage()).contains("ArrayEquals");
  }

  @Test
  public void filterWorks() {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ArrayEquals.class, BadShiftAmount.class, StaticQualifiedUsingExpression.class);

    assertScanner(ss.filter(input -> input.canonicalName().equals("BadShiftAmount")))
        .hasEnabledChecks(BadShiftAmount.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void applyOverridesWorksOnEmptySeverityMap() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ChainingConstructorIgnoresParameter.class,
            DepAnn.class,
            LongLiteralLowerCaseSuffix.class);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(Collections.emptyList());

    ScannerSupplier overridden = ss.applyOverrides(epOptions);
    assertScanner(overridden)
        .hasEnabledChecks(
            ChainingConstructorIgnoresParameter.class,
            DepAnn.class,
            LongLiteralLowerCaseSuffix.class);
  }

  @Test
  public void applyOverridesWorksOnEmptyFlagsMap() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckerClasses();
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(Collections.emptyList());

    ScannerSupplier overridden = ss.applyOverrides(epOptions);
    assertThat(overridden.getFlags().isEmpty()).isTrue();
  }

  @Test
  public void applyOverridesEnablesCheck() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
                ArrayEquals.class, BadShiftAmount.class, StaticQualifiedUsingExpression.class)
            .filter(Predicates.alwaysFalse()); // disables all checks

    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-Xep:ArrayEquals", "-Xep:BadShiftAmount"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasEnabledChecks(ArrayEquals.class, BadShiftAmount.class);
  }

  @Test
  public void applyOverridesEnableAllChecks() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
                ArrayEquals.class, BadShiftAmount.class, StaticQualifiedUsingExpression.class)
            .filter(Predicates.alwaysFalse()); // disables all checks

    assertScanner(ss).hasEnabledChecks(); // assert empty scanner has no enabled checks

    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-XepAllDisabledChecksAsWarnings"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasEnabledChecks(
            ArrayEquals.class, BadShiftAmount.class, StaticQualifiedUsingExpression.class);

    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-XepAllDisabledChecksAsWarnings", "-Xep:ArrayEquals:OFF"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasEnabledChecks(BadShiftAmount.class, StaticQualifiedUsingExpression.class);

    // The 'AllDisabledChecksAsWarnings' flag doesn't populate through to additional plugins
    assertScanner(
            ss.applyOverrides(epOptions)
                .plus(ScannerSupplier.fromBugCheckerClasses(DivZero.class).filter(t -> false)))
        .hasEnabledChecks(BadShiftAmount.class, StaticQualifiedUsingExpression.class);
  }

  @Test
  public void applyOverridesDisableAllChecks() throws Exception {
    // Create new scanner.
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ArrayEquals.class, BadShiftAmount.class, StaticQualifiedUsingExpression.class);

    // Make sure all checks in the scanner start enabled.
    assertScanner(ss)
        .hasEnabledChecks(
            ArrayEquals.class, BadShiftAmount.class, StaticQualifiedUsingExpression.class);

    // Apply the 'DisableAllChecks' flag, make sure all checks are disabled.
    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-XepDisableAllChecks"));

    assertScanner(ss.applyOverrides(epOptions)).hasEnabledChecks();

    // Don't suppress unsuppressible checks.
    ss = ss.plus(ScannerSupplier.fromBugCheckerClasses(RestrictedApiChecker.class));

    assertScanner(ss.applyOverrides(epOptions)).hasEnabledChecks(RestrictedApiChecker.class);

    // Apply 'DisableAllChecks' flag with another -Xep flag
    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-XepDisableAllChecks", "-Xep:ArrayEquals:ERROR"));

    // Make sure the severity flag overrides the global disable flag.
    assertScanner(ss.applyOverrides(epOptions))
        .hasEnabledChecks(RestrictedApiChecker.class, ArrayEquals.class);

    // Order matters. The DisableAllChecks flag should override all severities that come before it.
    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-Xep:ArrayEquals:ERROR", "-XepDisableAllChecks"));

    // Make sure the global disable flag overrides flags that come before it.
    assertScanner(ss.applyOverrides(epOptions)).hasEnabledChecks(RestrictedApiChecker.class);

    // The 'DisableAllChecks' flag doesn't populate through to additional plugins.
    assertScanner(
            ss.applyOverrides(epOptions).plus(ScannerSupplier.fromBugCheckerClasses(DivZero.class)))
        .hasEnabledChecks(RestrictedApiChecker.class, DivZero.class);
  }

  @Test
  public void applyOverridesDisableErrors() {
    // BadShiftAmount (error), ArrayEquals (unsuppressible error), StringEquality (warning)
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            BadShiftAmount.class, UnsuppressibleArrayEquals.class, StringEquality.class);

    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-XepAllErrorsAsWarnings"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasSeverities(
            ImmutableMap.of(
                "ArrayEquals", SeverityLevel.ERROR, // Unsuppressible, not demoted
                "BadShiftAmount", SeverityLevel.WARNING, // Demoted from error to warning
                "StringEquality", SeverityLevel.WARNING)); // Already warning, unaffected

    // Flags after AllErrorsAsWarnings flag should override it.
    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-XepAllErrorsAsWarnings", "-Xep:StringEquality:ERROR"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasSeverities(
            ImmutableMap.of(
                "ArrayEquals", SeverityLevel.ERROR,
                "BadShiftAmount", SeverityLevel.WARNING,
                "StringEquality", SeverityLevel.ERROR));

    // AllErrorsAsWarnings flag should override all error-level severity flags that come before it.
    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-Xep:StringEquality:ERROR", "-XepAllErrorsAsWarnings"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasSeverities(
            ImmutableMap.of(
                "ArrayEquals", SeverityLevel.ERROR,
                "BadShiftAmount", SeverityLevel.WARNING,
                "StringEquality", SeverityLevel.WARNING));

    // AllErrorsAsWarnings only overrides error-level severity flags.
    // That is, checks disabled before the flag are left disabled, not promoted to warnings.
    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-Xep:BadShiftAmount:OFF", "-XepAllErrorsAsWarnings"));

    assertScanner(ss.applyOverrides(epOptions))
        .hasSeverities(
            ImmutableMap.of(
                "ArrayEquals", SeverityLevel.ERROR,
                "StringEquality", SeverityLevel.WARNING));
    assertScanner(ss.applyOverrides(epOptions))
        .hasEnabledChecks(UnsuppressibleArrayEquals.class, StringEquality.class);
  }

  @Test
  public void applyOverridesDisableErrorsOnlyForEnabledChecks() {
    Supplier<ScannerSupplier> filteredScanner =
        () ->
            ScannerSupplier.fromBugCheckerClasses(
                    BadShiftAmount.class,
                    UnsuppressibleArrayEquals.class,
                    EqualsIncompatibleType.class)
                .filter(p -> !p.checkerClass().equals(EqualsIncompatibleType.class));

    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-XepAllErrorsAsWarnings"));

    assertScanner(filteredScanner.get().applyOverrides(epOptions))
        .hasEnabledChecks(UnsuppressibleArrayEquals.class, BadShiftAmount.class);

    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-XepAllErrorsAsWarnings", "-Xep:BadShiftAmount:OFF"));

    assertScanner(filteredScanner.get().applyOverrides(epOptions))
        .hasEnabledChecks(UnsuppressibleArrayEquals.class);
  }

  @Test
  public void applyOverridesDisablesChecks() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ChainingConstructorIgnoresParameter.class,
            DepAnn.class,
            LongLiteralLowerCaseSuffix.class);
    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of(
                "-Xep:LongLiteralLowerCaseSuffix:OFF",
                "-Xep:ChainingConstructorIgnoresParameter:OFF"));

    assertScanner(ss.applyOverrides(epOptions)).hasEnabledChecks(DepAnn.class);
  }

  @Test
  public void applyOverridesThrowsExceptionWhenDisablingNonDisablableCheck() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals.class);
    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-Xep:ArrayEquals:OFF"));

    InvalidCommandLineOptionException exception =
        expectThrows(InvalidCommandLineOptionException.class, () -> ss.applyOverrides(epOptions));
    assertThat(exception.getMessage()).contains("may not be disabled");
  }

  @Test
  public void applyOverridesThrowsExceptionWhenDemotingNonDisablableCheck() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals.class);
    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(ImmutableList.of("-Xep:ArrayEquals:WARN"));

    InvalidCommandLineOptionException exception =
        expectThrows(InvalidCommandLineOptionException.class, () -> ss.applyOverrides(epOptions));
    assertThat(exception.getMessage()).contains("may not be demoted to a warning");
  }

  @Test
  public void applyOverridesSucceedsWhenDisablingUnknownCheckAndIgnoreUnknownCheckNamesIsSet()
      throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckerClasses(ArrayEquals.class);
    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-XepIgnoreUnknownCheckNames", "-Xep:foo:OFF"));

    assertScanner(ss.applyOverrides(epOptions)).hasEnabledChecks(ArrayEquals.class);
  }

  @Test
  public void applyOverridesSetsSeverity() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
            BadShiftAmount.class, ChainingConstructorIgnoresParameter.class, StringEquality.class);
    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of(
                "-Xep:ChainingConstructorIgnoresParameter:WARN", "-Xep:StringEquality:ERROR"));
    ScannerSupplier overriddenScannerSupplier = ss.applyOverrides(epOptions);

    Map<String, SeverityLevel> expected =
        ImmutableMap.of(
            "BadShiftAmount", SeverityLevel.ERROR,
            "ChainingConstructorIgnoresParameter", SeverityLevel.WARNING,
            "StringEquality", SeverityLevel.ERROR);

    assertScanner(overriddenScannerSupplier).hasSeverities(expected);
  }

  @Test
  public void applyOverridesSetsFlags() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckerClasses();
    assertThat(ss.getFlags().isEmpty()).isTrue();

    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of(
                "-XepOpt:FirstFlag=overridden", "-XepOpt:SecondFlag=AValue", "-XepOpt:FirstFlag"));
    ScannerSupplier overriddenScannerSupplier = ss.applyOverrides(epOptions);

    Map<String, String> expected =
        ImmutableMap.of(
            "FirstFlag", "true",
            "SecondFlag", "AValue");

    assertThat(overriddenScannerSupplier.getFlags().getFlagsMap())
        .containsExactlyEntriesIn(expected);
  }

  @Test
  public void allChecksAsWarningsWorks() throws Exception {
    ScannerSupplier ss =
        ScannerSupplier.fromBugCheckerClasses(
                BadShiftAmount.class,
                ChainingConstructorIgnoresParameter.class,
                StringEquality.class)
            .filter(Predicates.alwaysFalse());
    assertScanner(ss).hasEnabledChecks(); // Everything's off

    ErrorProneOptions epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of("-Xep:StringEquality:OFF", "-XepAllDisabledChecksAsWarnings"));

    ScannerSupplier withOverrides = ss.applyOverrides(epOptions);
    assertScanner(withOverrides)
        .hasEnabledChecks(
            BadShiftAmount.class, ChainingConstructorIgnoresParameter.class, StringEquality.class);

    ImmutableMap<String, SeverityLevel> expectedSeverities =
        ImmutableMap.of(
            "BadShiftAmount",
            SeverityLevel.WARNING,
            "ChainingConstructorIgnoresParameter",
            SeverityLevel.WARNING,
            "StringEquality",
            SeverityLevel.WARNING);
    assertScanner(withOverrides).hasSeverities(expectedSeverities);

    epOptions =
        ErrorProneOptions.processArgs(
            ImmutableList.of(
                "-Xep:StringEquality:OFF",
                "-XepAllDisabledChecksAsWarnings",
                "-Xep:StringEquality:OFF"));

    withOverrides = ss.applyOverrides(epOptions);
    assertScanner(withOverrides)
        .hasEnabledChecks(BadShiftAmount.class, ChainingConstructorIgnoresParameter.class);

    expectedSeverities =
        ImmutableMap.of(
            "BadShiftAmount",
            SeverityLevel.WARNING,
            "ChainingConstructorIgnoresParameter",
            SeverityLevel.WARNING);
    assertScanner(withOverrides).hasSeverities(expectedSeverities);
  }

  private static class ScannerSupplierSubject
      extends Subject<ScannerSupplierSubject, ScannerSupplier> {
    ScannerSupplierSubject(FailureMetadata failureMetadata, ScannerSupplier scannerSupplier) {
      super(failureMetadata, scannerSupplier);
    }

    final void hasSeverities(Map<String, SeverityLevel> severities) {
      check().that(getSubject().severities()).containsExactlyEntriesIn(severities);
    }

    @SafeVarargs
    final void hasEnabledChecks(Class<? extends BugChecker>... bugCheckers) {
      check()
          .that(getSubject().getEnabledChecks())
          .containsExactlyElementsIn(getSuppliers(bugCheckers));
    }
  }

  private ScannerSupplierSubject assertScanner(ScannerSupplier scannerSupplier) {
    return assertAbout(SCANNER_SUBJECT_FACTORY).that(scannerSupplier);
  }

  private static final Subject.Factory<ScannerSupplierSubject, ScannerSupplier>
      SCANNER_SUBJECT_FACTORY = ScannerSupplierSubject::new;
}
