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

  private static final String IGNORE_UNKNOWN_CHECKS_FLAG = "-XepIgnoreUnknownCheckNames";
  private static final String FLAG_PREFIX = "-Xep:";
  private static final String DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG =
      "-XepDisableWarningsInGeneratedCode";

  /**
   * see {@link javax.tools.OptionChecker#isSupportedOption(String)}
   */
  public static int isSupportedOption(String option) {
    boolean isSupported =
        option.startsWith(FLAG_PREFIX)
            || option.equals(IGNORE_UNKNOWN_CHECKS_FLAG)
            || option.equals(DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG);
    return isSupported ? 0 : -1;
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

  private final ImmutableList<String> remainingArgs;
  private final ImmutableMap<String, Severity> severityMap;
  private final boolean ignoreUnknownChecks;
  private final boolean disableWarningsInGeneratedCode;

  private ErrorProneOptions(
      ImmutableMap<String, Severity> severityMap,
      ImmutableList<String> remainingArgs,
      boolean ignoreUnknownChecks,
      boolean disableWarningsInGeneratedCode) {
    this.severityMap = severityMap;
    this.remainingArgs = remainingArgs;
    this.ignoreUnknownChecks = ignoreUnknownChecks;
    this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
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

  private static class Builder {
    private boolean ignoreUnknownChecks = false;
    private boolean disableWarningsInGeneratedCode = false;
    private Map<String, Severity> severityMap = new HashMap<>();

    public void setIgnoreUnknownChecks(boolean ignoreUnknownChecks) {
      this.ignoreUnknownChecks = ignoreUnknownChecks;
    }

    public void setDisableWarningsInGeneratedCode(boolean disableWarningsInGeneratedCode) {
      this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    }

    public void putSeverity(String checkName, Severity severity) {
      severityMap.put(checkName, severity);
    }

    public ErrorProneOptions build(ImmutableList<String> outputArgs) {
      return new ErrorProneOptions(
          ImmutableMap.copyOf(severityMap),
          outputArgs,
          ignoreUnknownChecks,
          disableWarningsInGeneratedCode);
    }
  }

  private static final ErrorProneOptions EMPTY = new Builder().build(ImmutableList.<String>of());

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
  public static ErrorProneOptions processArgs(Iterable<String> args)
      throws InvalidCommandLineOptionException {
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
      if (arg.equals(IGNORE_UNKNOWN_CHECKS_FLAG)) {
        builder.setIgnoreUnknownChecks(true);
      } else if (arg.equals(DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG)) {
        builder.setDisableWarningsInGeneratedCode(true);
      } else if (arg.startsWith(FLAG_PREFIX)) {
        // Strip prefix
        String remaining = arg.substring(FLAG_PREFIX.length());
        // Split on ':'
        String[] parts = remaining.split(":");
        if (parts.length > 2 || parts[0].isEmpty()) {
          throw new InvalidCommandLineOptionException("invalid flag: " + arg);
        }
        String checkName = parts[0];
        Severity severity;
        if (parts.length == 1) {
          severity = Severity.DEFAULT;
        } else {  // parts.length == 2
          try {
            severity = Severity.valueOf(parts[1]);
          } catch (IllegalArgumentException e) {
            throw new InvalidCommandLineOptionException("invalid flag: " + arg);
          }
        }
        builder.putSeverity(checkName, severity);
      } else {
        outputArgs.add(arg);
      }
    }

    return builder.build(outputArgs.build());
  }

  /**
   * Given a list of command-line arguments, produce the corresponding {@link ErrorProneOptions}
   * instance.
   *
   * @param args command-line arguments
   * @return an {@link ErrorProneOptions} instance encapsulating the given arguments
   * @throws InvalidCommandLineOptionException if an error-prone option is invalid
   */
  public static ErrorProneOptions processArgs(String[] args)
      throws InvalidCommandLineOptionException {
    Preconditions.checkNotNull(args);
    return processArgs(Arrays.asList(args));
  }
}
