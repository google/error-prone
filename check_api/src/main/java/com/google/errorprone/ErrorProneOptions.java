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

package com.google.errorprone;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes command-line options specific to error-prone.
 *
 * <p>error-prone lets the user enable and disable specific checks as well as override their
 * built-in severity levels (warning vs. error).
 *
 * <p>A valid error-prone command-line option looks like:<br>
 * <pre>{@code
 * -Xep:<checkName>[:severity]
 * }</pre>
 *
 * <p>{@code checkName} is required and is the canonical name of the check, e.g. "StringEquality".
 * {@code severity} is one of {"OFF", "WARN", "ERROR"}.  Multiple flags must be passed to
 * enable or disable multiple checks.  The last flag for a specific check wins.
 *
 * <p>Examples of usage follow:<br>
 * <pre>{@code
 * -Xep:StringEquality  [turns on StringEquality check with the severity level from its BugPattern
 *                       annotation]
 * -Xep:StringEquality:OFF  [turns off StringEquality check]
 * -Xep:StringEquality:WARN  [turns on StringEquality check as a warning]
 * -Xep:StringEquality:ERROR  [turns on StringEquality check as an error]
 * -Xep:StringEquality:OFF -Xep:StringEquality  [turns on StringEquality check]
 * }</pre>
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ErrorProneOptions {

  private static final String CUSTOM_ENABLEMENT_PREFIX = "-Xep:";
  private static final String PATCH_FILE_PREFIX = "-XepPatch:";
  private static final String ERRORS_AS_WARNINGS_FLAG = "-XepAllErrorsAsWarnings";
  private static final String ENABLE_ALL_CHECKS = "-XepAllDisabledChecksAsWarnings";
  private static final String IGNORE_UNKNOWN_CHECKS_FLAG = "-XepIgnoreUnknownCheckNames";
  private static final String DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG =
      "-XepDisableWarningsInGeneratedCode";

  /**
   * see {@link javax.tools.OptionChecker#isSupportedOption(String)}
   */
  public static int isSupportedOption(String option) {
    boolean isSupported =
        option.startsWith(CUSTOM_ENABLEMENT_PREFIX)
            || option.startsWith(PATCH_FILE_PREFIX)
            || option.equals(IGNORE_UNKNOWN_CHECKS_FLAG)
            || option.equals(DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG)
            || option.equals(ERRORS_AS_WARNINGS_FLAG)
            || option.equals(ENABLE_ALL_CHECKS);
    return isSupported ? 0 : -1;
  }

  public boolean isEnableAllChecks() {
    return enableAllChecks;
  }

  /**
   * Severity levels for an error-prone check that define how the check results should be
   * presented.
   */
  public enum Severity {
    DEFAULT,    // whatever is specified in the @BugPattern annotation
    OFF,
    WARN,
    ERROR
  }

  @AutoValue
  abstract static class PatchingOptions {
    abstract boolean doRefactor();

    abstract boolean inPlace();

    abstract String baseDirectory();

    static PatchingOptions nope() {
      return new AutoValue_ErrorProneOptions_PatchingOptions(false, false, "");
    }

    static PatchingOptions doInPlace() {
      return new AutoValue_ErrorProneOptions_PatchingOptions(true, true, "");
    }

    static PatchingOptions baseDirectory(String baseDirectory) {
      return new AutoValue_ErrorProneOptions_PatchingOptions(true, false, baseDirectory);
    }
  }

  private final ImmutableList<String> remainingArgs;
  private final ImmutableMap<String, Severity> severityMap;
  private final boolean ignoreUnknownChecks;
  private final boolean disableWarningsInGeneratedCode;

  public boolean isDropErrorsToWarnings() {
    return dropErrorsToWarnings;
  }

  private final boolean dropErrorsToWarnings;
  private final boolean enableAllChecks;
  private final PatchingOptions patchingOptions;

  private ErrorProneOptions(
      ImmutableMap<String, Severity> severityMap,
      ImmutableList<String> remainingArgs,
      boolean ignoreUnknownChecks,
      boolean disableWarningsInGeneratedCode,
      boolean dropErrorsToWarnings,
      boolean enableAllChecks,
      PatchingOptions patchingOptions) {
    this.severityMap = severityMap;
    this.remainingArgs = remainingArgs;
    this.ignoreUnknownChecks = ignoreUnknownChecks;
    this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    this.dropErrorsToWarnings = dropErrorsToWarnings;
    this.enableAllChecks = enableAllChecks;
    this.patchingOptions = patchingOptions;
  }

  public String[] getRemainingArgs() {
    return remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  public ImmutableMap<String, Severity> getSeverityMap() {
    return severityMap;
  }

  public boolean ignoreUnknownChecks() {
    return ignoreUnknownChecks;
  }

  public boolean disableWarningsInGeneratedCode() {
    return disableWarningsInGeneratedCode;
  }

  public PatchingOptions patchingOptions() {
    return patchingOptions;
  }

  private static class Builder {
    private boolean ignoreUnknownChecks = false;
    private boolean disableWarningsInGeneratedCode = false;
    private boolean dropWarningsToErrors = false;
    private boolean enableAllChecks = false;
    private Map<String, Severity> severityMap = new HashMap<>();
    private PatchingOptions patchingOptions = PatchingOptions.nope();

    public void setIgnoreUnknownChecks(boolean ignoreUnknownChecks) {
      this.ignoreUnknownChecks = ignoreUnknownChecks;
    }

    public void setDisableWarningsInGeneratedCode(boolean disableWarningsInGeneratedCode) {
      this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    }

    public void setDropWarningsToErrors(boolean dropWarningsToErrors) {
      this.dropWarningsToErrors = dropWarningsToErrors;
    }

    public void putSeverity(String checkName, Severity severity) {
      severityMap.put(checkName, severity);
    }

    public void setEnableAllChecks(boolean enableAllChecks) {
      this.enableAllChecks = enableAllChecks;
    }

    public void setPatchingOptions(PatchingOptions patchingOptions) {
      this.patchingOptions = patchingOptions;
    }

    public ErrorProneOptions build(ImmutableList<String> outputArgs) {
      return new ErrorProneOptions(
          ImmutableMap.copyOf(severityMap),
          outputArgs,
          ignoreUnknownChecks,
          disableWarningsInGeneratedCode,
          dropWarningsToErrors,
          enableAllChecks,
          patchingOptions);
    }
  }

  private static final ErrorProneOptions EMPTY = new Builder().build(ImmutableList.of());

  public static ErrorProneOptions empty() {
    return EMPTY;
  }

  /**
   * Given a list of command-line arguments, produce the corresponding {@link ErrorProneOptions}
   * instance.
   *
   * @param args command-line arguments
   * @return an {@link ErrorProneOptions} instance encapsulating the given arguments
   * @throws InvalidCommandLineOptionException if an error-prone option is invalid
   */
  public static ErrorProneOptions processArgs(Iterable<String> args) {
    Preconditions.checkNotNull(args);
    ImmutableList.Builder<String> outputArgs = ImmutableList.builder();

    /* By default, we throw an error when an unknown option is passed in, if for example you
     * try to disable a check that doesn't match any of the known checks.  This catches typos from
     * the command line.
     *
     * You can pass the IGNORE_UNKNOWN_CHECKS_FLAG to opt-out of that checking.  This allows you to
     * use command lines from different versions of error-prone interchangably.
     */
    Builder builder = new Builder();
    for (String arg : args) {
      switch (arg) {
        case IGNORE_UNKNOWN_CHECKS_FLAG:
          builder.setIgnoreUnknownChecks(true);
          break;
        case DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG:
          builder.setDisableWarningsInGeneratedCode(true);
          break;
        case ERRORS_AS_WARNINGS_FLAG:
          builder.setDropWarningsToErrors(true);
          break;
        case ENABLE_ALL_CHECKS:
          builder.setEnableAllChecks(true);
          break;
        default:
          if (arg.startsWith(CUSTOM_ENABLEMENT_PREFIX)) {
            parseCustomFlagIntoOptionsBuilder(builder, arg);
          } else if (arg.startsWith(PATCH_FILE_PREFIX)) {
            parsePatchArgIntoBuilder(builder, arg);
          } else {
            outputArgs.add(arg);
          }
      }
    }

    return builder.build(outputArgs.build());
  }

  private static void parsePatchArgIntoBuilder(Builder builder, String arg) {
    String remaining = arg.substring(PATCH_FILE_PREFIX.length());
    if (remaining.equals("IN_PLACE")) {
      builder.setPatchingOptions(PatchingOptions.doInPlace());
    } else {
      if (remaining.isEmpty()) {
        throw new InvalidCommandLineOptionException("invalid flag: " + arg);
      }
      builder.setPatchingOptions(PatchingOptions.baseDirectory(remaining));
    }
  }

  private static void parseCustomFlagIntoOptionsBuilder(Builder builder, String arg) {
    // Strip prefix
    String remaining = arg.substring(CUSTOM_ENABLEMENT_PREFIX.length());
    // Split on ':'
    String[] parts = remaining.split(":");
    if (parts.length > 2 || parts[0].isEmpty()) {
      throw new InvalidCommandLineOptionException("invalid flag: " + arg);
    }
    String checkName = parts[0];
    Severity severity;
    if (parts.length == 1) {
      severity = Severity.DEFAULT;
    } else { // parts.length == 2
      try {
        severity = Severity.valueOf(parts[1]);
      } catch (IllegalArgumentException e) {
        throw new InvalidCommandLineOptionException("invalid flag: " + arg);
      }
    }
    builder.putSeverity(checkName, severity);
  }

  /**
   * Given a list of command-line arguments, produce the corresponding {@link ErrorProneOptions}
   * instance.
   *
   * @param args command-line arguments
   * @return an {@link ErrorProneOptions} instance encapsulating the given arguments
   * @throws InvalidCommandLineOptionException if an error-prone option is invalid
   */
  public static ErrorProneOptions processArgs(String[] args) {
    Preconditions.checkNotNull(args);
    return processArgs(Arrays.asList(args));
  }
}
