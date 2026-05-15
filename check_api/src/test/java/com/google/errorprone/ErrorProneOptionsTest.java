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

package com.google.errorprone;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.apply.ImportOrganizer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@code ErrorProneOptions}.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class ErrorProneOptionsTest {

  @Test
  public void nonErrorProneFlagsPlacedInRemainingArgs() {
    String[] args = {"-nonErrorProneFlag", "value"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    assertThat(options.getRemainingArgs()).containsExactlyElementsIn(args);
  }

  @Test
  public void malformedOptionThrowsProperException() {
    List<String> badArgs =
        Arrays.asList(
            "-Xep:Foo:WARN:jfkdlsdf", // too many parts
            "-Xep:", // no check name
            "-Xep:Foo:FJDKFJSD"); // nonexistent severity level

    badArgs.forEach(
        arg -> {
          InvalidCommandLineOptionException expected =
              assertThrows(
                  InvalidCommandLineOptionException.class,
                  () -> ErrorProneOptions.processArgs(Arrays.asList(arg)));
          assertThat(expected).hasMessageThat().contains("invalid flag");
        });
  }

  @Test
  public void handlesErrorProneSeverityFlags() {
    String[] args1 = {"-Xep:Check1"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args1);
    ImmutableMap<String, Severity> expectedSeverityMap =
        ImmutableMap.of("Check1", Severity.DEFAULT);
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);

    String[] args2 = {"-Xep:Check1", "-Xep:Check2:OFF", "-Xep:Check3:WARN"};
    options = ErrorProneOptions.processArgs(args2);
    expectedSeverityMap =
        ImmutableMap.<String, Severity>builder()
            .put("Check1", Severity.DEFAULT)
            .put("Check2", Severity.OFF)
            .put("Check3", Severity.WARN)
            .buildOrThrow();
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }

  @Test
  public void handlesErrorProneCustomFlags() {
    String[] args = {"-XepOpt:Flag1", "-XepOpt:Flag2=Value2", "-XepOpt:Flag3=a,b,c"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    ImmutableMap<String, String> expectedFlagsMap =
        ImmutableMap.<String, String>builder()
            .put("Flag1", "true")
            .put("Flag2", "Value2")
            .put("Flag3", "a,b,c")
            .buildOrThrow();
    assertThat(options.getFlags().getFlagsMap()).isEqualTo(expectedFlagsMap);
  }

  @Test
  public void combineErrorProneFlagsWithNonErrorProneFlags() {
    String[] args = {
      "-classpath",
      "/this/is/classpath",
      "-verbose",
      "-Xep:Check1:WARN",
      "-XepOpt:Check1:Flag1=Value1",
      "-Xep:Check2:ERROR"
    };
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    String[] expectedRemainingArgs = {"-classpath", "/this/is/classpath", "-verbose"};
    assertThat(options.getRemainingArgs()).containsExactlyElementsIn(expectedRemainingArgs);
    ImmutableMap<String, Severity> expectedSeverityMap =
        ImmutableMap.<String, Severity>builder()
            .put("Check1", Severity.WARN)
            .put("Check2", Severity.ERROR)
            .buildOrThrow();
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
    ImmutableMap<String, String> expectedFlagsMap = ImmutableMap.of("Check1:Flag1", "Value1");
    assertThat(options.getFlags().getFlagsMap()).containsExactlyEntriesIn(expectedFlagsMap);
  }

  @Test
  public void lastSeverityFlagWins() {
    String[] args = {"-Xep:Check1:ERROR", "-Xep:Check1:OFF"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    ImmutableMap<String, Severity> expectedSeverityMap = ImmutableMap.of("Check1", Severity.OFF);
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }

  @Test
  public void lastCustomFlagWins() {
    String[] args = {"-XepOpt:Flag1=First", "-XepOpt:Flag1=Second"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    ImmutableMap<String, String> expectedFlagsMap = ImmutableMap.of("Flag1", "Second");
    assertThat(options.getFlags().getFlagsMap()).containsExactlyEntriesIn(expectedFlagsMap);
  }

  @Test
  public void recognizesAllChecksAsWarnings() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepAllDisabledChecksAsWarnings"});
    assertThat(options.isEnableAllChecksAsWarnings()).isTrue();
  }

  @Test
  public void recognizesDemoteErrorToWarning() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepAllErrorsAsWarnings"});
    assertThat(options.isDropErrorsToWarnings()).isTrue();
  }

  @Test
  public void recognizesAllSuggestionsAsWarnings() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepAllSuggestionsAsWarnings"});
    assertThat(options.isSuggestionsAsWarnings()).isTrue();
  }

  @Test
  public void recognizesDisableAllChecks() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepDisableAllChecks"});
    assertThat(options.isDisableAllChecks()).isTrue();
  }

  @Test
  public void recognizesCompilingTestOnlyCode() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepCompilingTestOnlyCode"});
    assertThat(options.isTestOnlyTarget()).isTrue();
  }

  @Test
  public void recognizesCompilingPubliclyVisibleCode() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepCompilingPubliclyVisibleCode"});
    assertThat(options.isPubliclyVisibleTarget()).isTrue();
  }

  @Test
  public void recognizesDisableAllWarnings() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepDisableAllWarnings"});
    assertThat(options.isDisableAllWarnings()).isTrue();
  }

  @Test
  public void recognizesVisitSuppressedCode() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepIgnoreSuppressionAnnotations"});
    assertThat(options.isIgnoreSuppressionAnnotations()).isTrue();
  }

  @Test
  public void recognizesExcludedPaths() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(
            new String[] {"-XepExcludedPaths:(.*/)?(build/generated|other_output)/.*\\.java"});
    Pattern excludedPattern = options.getExcludedPattern();
    assertThat(excludedPattern).isNotNull();
    assertThat(excludedPattern.matcher("fizz/build/generated/Gen.java").matches()).isTrue();
    assertThat(excludedPattern.matcher("fizz/bazz/generated/Gen.java").matches()).isFalse();
    assertThat(excludedPattern.matcher("fizz/abuild/generated/Gen.java").matches()).isFalse();
    assertThat(excludedPattern.matcher("other_output/Gen.java").matches()).isTrue();
    assertThat(excludedPattern.matcher("foo/other_output/subdir/Gen.java").matches()).isTrue();
    assertThat(excludedPattern.matcher("foo/other_output/subdir/Gen.cpp").matches()).isFalse();
  }

  @Test
  public void recognizesPatch() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(
            new String[] {"-XepPatchLocation:IN_PLACE", "-XepPatchChecks:FooBar,MissingOverride"});
    assertThat(options.patchingOptions().doRefactor()).isTrue();
    assertThat(options.patchingOptions().inPlace()).isTrue();
    assertThat(options.patchingOptions().namedCheckers())
        .containsExactly("MissingOverride", "FooBar");
    assertThat(options.patchingOptions().customRefactorer()).isAbsent();

    options =
        ErrorProneOptions.processArgs(
            new String[] {
              "-XepPatchLocation:/some/base/dir", "-XepPatchChecks:FooBar,MissingOverride"
            });
    assertThat(options.patchingOptions().doRefactor()).isTrue();
    assertThat(options.patchingOptions().inPlace()).isFalse();
    assertThat(options.patchingOptions().baseDirectory()).isEqualTo("/some/base/dir");
    assertThat(options.patchingOptions().namedCheckers())
        .containsExactly("MissingOverride", "FooBar");
    assertThat(options.patchingOptions().customRefactorer()).isAbsent();

    options = ErrorProneOptions.processArgs(new String[] {});
    assertThat(options.patchingOptions().doRefactor()).isFalse();
  }

  @Test
  public void throwsExceptionWithBadPatchArgs() {
    assertThrows(
        InvalidCommandLineOptionException.class,
        () ->
            ErrorProneOptions.processArgs(new String[] {"-XepPatchChecks:FooBar,MissingOverride"}));
  }

  @Test
  public void recognizesRefaster() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(
            new String[] {"-XepPatchChecks:refaster:/foo/bar", "-XepPatchLocation:IN_PLACE"});
    assertThat(options.patchingOptions().doRefactor()).isTrue();
    assertThat(options.patchingOptions().inPlace()).isTrue();
    assertThat(options.patchingOptions().customRefactorer()).isPresent();
  }

  @Test
  public void understandsEmptySetOfNamedCheckers() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepPatchLocation:IN_PLACE"});
    assertThat(options.patchingOptions().doRefactor()).isTrue();
    assertThat(options.patchingOptions().inPlace()).isTrue();
    assertThat(options.patchingOptions().namedCheckers()).isEmpty();
    assertThat(options.patchingOptions().customRefactorer()).isAbsent();

    options =
        ErrorProneOptions.processArgs(
            new String[] {"-XepPatchLocation:IN_PLACE", "-XepPatchChecks:"});
    assertThat(options.patchingOptions().doRefactor()).isTrue();
    assertThat(options.patchingOptions().inPlace()).isTrue();
    assertThat(options.patchingOptions().namedCheckers()).isEmpty();
    assertThat(options.patchingOptions().customRefactorer()).isAbsent();
  }

  @Test
  public void importOrder_staticFirst() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepPatchImportOrder:static-first"});
    assertThat(options.patchingOptions().importOrganizer())
        .isSameInstanceAs(ImportOrganizer.STATIC_FIRST_ORGANIZER);
  }

  @Test
  public void importOrder_staticLast() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepPatchImportOrder:static-last"});
    assertThat(options.patchingOptions().importOrganizer())
        .isSameInstanceAs(ImportOrganizer.STATIC_LAST_ORGANIZER);
  }

  @Test
  public void importOrder_androidStaticFirst() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepPatchImportOrder:android-static-first"});
    assertThat(options.patchingOptions().importOrganizer())
        .isSameInstanceAs(ImportOrganizer.ANDROID_STATIC_FIRST_ORGANIZER);
  }

  @Test
  public void importOrder_androidStaticLast() {
    ErrorProneOptions options =
        ErrorProneOptions.processArgs(new String[] {"-XepPatchImportOrder:android-static-last"});
    assertThat(options.patchingOptions().importOrganizer())
        .isSameInstanceAs(ImportOrganizer.ANDROID_STATIC_LAST_ORGANIZER);
  }

  @Test
  public void noSuchXepFlag() {
    assertThrows(
        InvalidCommandLineOptionException.class,
        () -> ErrorProneOptions.processArgs(new String[] {"-XepNoSuchFlag"}));
  }

  @Test
  public void severityOrder() {
    for (Collection<String> permutation :
        Collections2.permutations(ImmutableList.of("A", "B", "C"))) {
      ImmutableMap<String, Severity> severityMap =
          permutation.stream().collect(toImmutableMap(x -> x, x -> Severity.ERROR));
      ErrorProneOptions options =
          ErrorProneOptions.processArgs(
              permutation.stream()
                  .map(x -> String.format("-Xep:%s:ERROR", x))
                  .collect(toImmutableList()));
      assertThat(options.getSeverityMap()).containsExactlyEntriesIn(severityMap).inOrder();
    }
  }

  @Test
  public void processArgumentsFileParsing() throws IOException {
    File source = File.createTempFile("ep_argfile_", ".cfg");
    source.deleteOnExit();
    Files.write(
        source.toPath(),
        ImmutableList.of(
            "# Line comment",
            "-Xep:UnicodeEscape:OFF",
            "-Xep:InvalidBlockTag:OFF # inline comment",
            "-Xep:JavaUtilDate:OFF",
            "# Several flags on a single line are acceptable:",
            "-Xep:LabelledBreakTarget:OFF \t -Xep:JUnit4TestNotRun:OFF"
                + "   -Xep:ComparableType:OFF",
            "-Xep:EqualsHashCode:WARN",
            "-Xep:ReturnValueIgnored:WARN",
            "  # Indents and trailing, comment also indented:",
            "\t\t  \t-Xep:ArrayToString:WARN",
            "  -Xep:MisusedDayOfYear:WARN\t   ",
            "  -Xep:SelfComparison:WARN   ",
            "-Xep:MisusedWeekYear:WARN"),
        UTF_8);

    String[] args = {"@" + source.getAbsolutePath()};
    ImmutableMap<String, Severity> expectedSeverityMap =
        ImmutableMap.<String, Severity>builder()
            .put("UnicodeEscape", Severity.OFF)
            .put("InvalidBlockTag", Severity.OFF)
            .put("JavaUtilDate", Severity.OFF)
            .put("LabelledBreakTarget", Severity.OFF)
            .put("JUnit4TestNotRun", Severity.OFF)
            .put("ComparableType", Severity.OFF)
            .put("EqualsHashCode", Severity.WARN)
            .put("ReturnValueIgnored", Severity.WARN)
            .put("ArrayToString", Severity.WARN)
            .put("MisusedDayOfYear", Severity.WARN)
            .put("SelfComparison", Severity.WARN)
            .put("MisusedWeekYear", Severity.WARN)
            .buildOrThrow();

    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }

  @Test
  public void processArgumentsFileOverrides() throws IOException {
    File source1 = File.createTempFile("ep_argfile_", ".cfg");
    source1.deleteOnExit();
    File source2 = File.createTempFile("ep_argfile_", ".cfg");
    source2.deleteOnExit();

    Files.write(
        source1.toPath(),
        ImmutableList.of(
            "-Xep:InvalidParam:WARN", "-Xep:JUnit4TestNotRun:WARN", "-Xep:EmptyBlockTag:WARN"),
        UTF_8);
    Files.write(
        source2.toPath(),
        ImmutableList.of(
            "-Xep:EffectivelyPrivate:OFF",
            "-Xep:ReturnValueIgnored:OFF",
            "-Xep:EmptyBlockTag:OFF",
            "-Xep:IntLongMath:OFF"),
        UTF_8);

    String[] args = {
      "-Xep:InvalidParam:ERROR",
      "-Xep:EffectivelyPrivate:ERROR",
      "-Xep:UnicodeEscape:ERROR",
      "-Xep:JavaUtilDate:ERROR",
      "@" + source1.getAbsolutePath(),
      "-Xep:UnicodeEscape:ERROR",
      "-Xep:JUnit4TestNotRun:ERROR",
      "@" + source2.getAbsolutePath(),
      "-Xep:JavaUtilDate:ERROR",
      "-Xep:IntLongMath:ERROR"
    };

    // To make the result easier to follow, the `args` set flags to ERROR,
    // the first config file uses WARN, and the second config file uses OFF.
    ImmutableMap<String, Severity> expectedSeverityMap =
        ImmutableMap.<String, Severity>builder()
            .put("InvalidParam", Severity.WARN)
            .put("EffectivelyPrivate", Severity.OFF)
            .put("UnicodeEscape", Severity.ERROR)
            .put("JavaUtilDate", Severity.ERROR)
            .put("JUnit4TestNotRun", Severity.ERROR)
            .put("EmptyBlockTag", Severity.OFF)
            .put("ReturnValueIgnored", Severity.OFF)
            .put("IntLongMath", Severity.ERROR)
            .buildOrThrow();

    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }

  @Test
  public void processArgumentsFileRefOtherConfig() throws IOException {
    File source = File.createTempFile("ep_argfile_", ".cfg");
    source.deleteOnExit();
    Files.write(
        source.toPath(),
        ImmutableList.of("-Xep:InvalidParam:WARN", "@other.cfg", "-Xep:EmptyBlockTag:WARN"),
        UTF_8);
    assertThrows(
        InvalidCommandLineOptionException.class,
        () -> ErrorProneOptions.processArgs(new String[] {"@" + source.getAbsolutePath()}));
  }

  @Test
  public void processArgumentsFileMissing() {
    assertThrows(
        InvalidCommandLineOptionException.class,
        () -> ErrorProneOptions.processArgs(new String[] {"@test_cfg_is_missing.cfg"}));
  }
}
