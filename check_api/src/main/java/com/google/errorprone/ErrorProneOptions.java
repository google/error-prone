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

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.apply.ImportOrganizer;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Processes command-line options specific to error-prone.
 *
 * <p>Documentation for the available flags are available at https://errorprone.info/docs/flags
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ErrorProneOptions {

  private static final String PREFIX = "-Xep";
  private static final String SEVERITY_PREFIX = "-Xep:";
  private static final String PATCH_CHECKS_PREFIX = "-XepPatchChecks:";
  private static final String PATCH_OUTPUT_LOCATION = "-XepPatchLocation:";
  private static final String PATCH_IMPORT_ORDER_PREFIX = "-XepPatchImportOrder:";
  private static final String EXCLUDED_PATHS_PREFIX = "-XepExcludedPaths:";
  private static final String IGNORE_LARGE_CODE_GENERATORS = "-XepIgnoreLargeCodeGenerators:";

  private static final String ERRORS_AS_WARNINGS_FLAG = "-XepAllErrorsAsWarnings";
  private static final String ENABLE_ALL_CHECKS = "-XepAllDisabledChecksAsWarnings";
  private static final String IGNORE_SUPPRESSION_ANNOTATIONS = "-XepIgnoreSuppressionAnnotations";
  private static final String DISABLE_ALL_CHECKS = "-XepDisableAllChecks";
  private static final String DISABLE_ALL_WARNINGS = "-XepDisableAllWarnings";
  private static final String IGNORE_UNKNOWN_CHECKS_FLAG = "-XepIgnoreUnknownCheckNames";
  private static final String DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG =
      "-XepDisableWarningsInGeneratedCode";
  private static final String COMPILING_TEST_ONLY_CODE = "-XepCompilingTestOnlyCode";

  /** see {@link javax.tools.OptionChecker#isSupportedOption(String)} */
  public static int isSupportedOption(String option) {
    boolean isSupported =
        option.startsWith(SEVERITY_PREFIX)
            || option.startsWith(ErrorProneFlags.PREFIX)
            || option.startsWith(PATCH_OUTPUT_LOCATION)
            || option.startsWith(PATCH_CHECKS_PREFIX)
            || option.startsWith(EXCLUDED_PATHS_PREFIX)
            || option.equals(IGNORE_UNKNOWN_CHECKS_FLAG)
            || option.equals(DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG)
            || option.equals(ERRORS_AS_WARNINGS_FLAG)
            || option.equals(ENABLE_ALL_CHECKS)
            || option.equals(DISABLE_ALL_CHECKS)
            || option.equals(IGNORE_SUPPRESSION_ANNOTATIONS)
            || option.equals(COMPILING_TEST_ONLY_CODE)
            || option.equals(DISABLE_ALL_WARNINGS);
    return isSupported ? 0 : -1;
  }

  public boolean isEnableAllChecksAsWarnings() {
    return enableAllChecksAsWarnings;
  }

  public boolean isDisableAllChecks() {
    return disableAllChecks;
  }

  /**
   * Severity levels for an error-prone check that define how the check results should be presented.
   */
  public enum Severity {
    DEFAULT, // whatever is specified in the @BugPattern annotation
    OFF,
    WARN,
    ERROR
  }

  @AutoValue
  abstract static class PatchingOptions {
    final boolean doRefactor() {
      return inPlace() || !baseDirectory().isEmpty();
    }

    abstract ImmutableSet<String> namedCheckers();

    abstract boolean inPlace();

    abstract String baseDirectory();

    abstract Optional<Supplier<CodeTransformer>> customRefactorer();

    abstract ImportOrganizer importOrganizer();

    static Builder builder() {
      return new AutoValue_ErrorProneOptions_PatchingOptions.Builder()
          .baseDirectory("")
          .inPlace(false)
          .namedCheckers(ImmutableSet.of())
          .importOrganizer(ImportOrganizer.STATIC_FIRST_ORGANIZER);
    }

    @AutoValue.Builder
    abstract static class Builder {

      abstract Builder namedCheckers(ImmutableSet<String> checkers);

      abstract Builder inPlace(boolean inPlace);

      abstract Builder baseDirectory(String baseDirectory);

      abstract Builder customRefactorer(Supplier<CodeTransformer> refactorer);

      abstract Builder importOrganizer(ImportOrganizer importOrganizer);

      abstract PatchingOptions autoBuild();

      final PatchingOptions build() {

        PatchingOptions patchingOptions = autoBuild();

        // If anything is specified, then (checkers or refaster) and output must be set.
        if ((!patchingOptions.namedCheckers().isEmpty()
                || patchingOptions.customRefactorer().isPresent())
            ^ patchingOptions.doRefactor()) {
          throw new InvalidCommandLineOptionException(
              "-XepPatchChecks and -XepPatchLocation must be specified together");
        }
        return patchingOptions;
      }
    }
  }

  private final ImmutableList<String> remainingArgs;
  private final ImmutableMap<String, Severity> severityMap;
  private final boolean ignoreUnknownChecks;
  private final boolean disableWarningsInGeneratedCode;
  private final boolean disableAllWarnings;
  private final boolean dropErrorsToWarnings;
  private final boolean enableAllChecksAsWarnings;
  private final boolean disableAllChecks;
  private final boolean isTestOnlyTarget;
  private final ErrorProneFlags flags;
  private final PatchingOptions patchingOptions;
  private final Pattern excludedPattern;
  private final boolean ignoreSuppressionAnnotations;
  private final boolean ignoreLargeCodeGenerators;

  private ErrorProneOptions(
      ImmutableMap<String, Severity> severityMap,
      ImmutableList<String> remainingArgs,
      boolean ignoreUnknownChecks,
      boolean disableWarningsInGeneratedCode,
      boolean disableAllWarnings,
      boolean dropErrorsToWarnings,
      boolean enableAllChecksAsWarnings,
      boolean disableAllChecks,
      boolean isTestOnlyTarget,
      ErrorProneFlags flags,
      PatchingOptions patchingOptions,
      Pattern excludedPattern,
      boolean ignoreSuppressionAnnotations,
      boolean ignoreLargeCodeGenerators) {
    this.severityMap = severityMap;
    this.remainingArgs = remainingArgs;
    this.ignoreUnknownChecks = ignoreUnknownChecks;
    this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    this.disableAllWarnings = disableAllWarnings;
    this.dropErrorsToWarnings = dropErrorsToWarnings;
    this.enableAllChecksAsWarnings = enableAllChecksAsWarnings;
    this.disableAllChecks = disableAllChecks;
    this.isTestOnlyTarget = isTestOnlyTarget;
    this.flags = flags;
    this.patchingOptions = patchingOptions;
    this.excludedPattern = excludedPattern;
    this.ignoreSuppressionAnnotations = ignoreSuppressionAnnotations;
    this.ignoreLargeCodeGenerators = ignoreLargeCodeGenerators;
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

  public boolean isDisableAllWarnings() {
    return disableAllWarnings;
  }

  public boolean isDropErrorsToWarnings() {
    return dropErrorsToWarnings;
  }

  public boolean isTestOnlyTarget() {
    return isTestOnlyTarget;
  }

  public boolean isIgnoreSuppressionAnnotations() {
    return ignoreSuppressionAnnotations;
  }

  public boolean ignoreLargeCodeGenerators() {
    return ignoreLargeCodeGenerators;
  }

  public ErrorProneFlags getFlags() {
    return flags;
  }

  public PatchingOptions patchingOptions() {
    return patchingOptions;
  }

  public Pattern getExcludedPattern() {
    return excludedPattern;
  }

  private static class Builder {
    private boolean ignoreUnknownChecks = false;
    private boolean disableAllWarnings = false;
    private boolean disableWarningsInGeneratedCode = false;
    private boolean dropErrorsToWarnings = false;
    private boolean enableAllChecksAsWarnings = false;
    private boolean disableAllChecks = false;
    private boolean isTestOnlyTarget = false;
    private boolean ignoreSuppressionAnnotations = false;
    private boolean ignoreLargeCodeGenerators = true;
    private Map<String, Severity> severityMap = new HashMap<>();
    private final ErrorProneFlags.Builder flagsBuilder = ErrorProneFlags.builder();
    private final PatchingOptions.Builder patchingOptionsBuilder = PatchingOptions.builder();
    private Pattern excludedPattern;

    private void parseSeverity(String arg) {
      // Strip prefix
      String remaining = arg.substring(SEVERITY_PREFIX.length());
      // Split on ':'
      List<String> parts = Splitter.on(':').splitToList(remaining);
      if (parts.size() > 2 || parts.get(0).isEmpty()) {
        throw new InvalidCommandLineOptionException("invalid flag: " + arg);
      }
      String checkName = parts.get(0);
      Severity severity;
      if (parts.size() == 1) {
        severity = Severity.DEFAULT;
      } else { // parts.length == 2
        try {
          severity = Severity.valueOf(parts.get(1));
        } catch (IllegalArgumentException e) {
          throw new InvalidCommandLineOptionException("invalid flag: " + arg);
        }
      }
      severityMap.put(checkName, severity);
    }

    public void parseFlag(String flag) {
      flagsBuilder.parseFlag(flag);
    }

    public void setIgnoreSuppressionAnnotations(boolean ignoreSuppressionAnnotations) {
      this.ignoreSuppressionAnnotations = ignoreSuppressionAnnotations;
    }

    public void setIgnoreUnknownChecks(boolean ignoreUnknownChecks) {
      this.ignoreUnknownChecks = ignoreUnknownChecks;
    }

    public void setDisableWarningsInGeneratedCode(boolean disableWarningsInGeneratedCode) {
      this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    }

    public void setDropErrorsToWarnings(boolean dropErrorsToWarnings) {
      severityMap.entrySet().stream()
          .filter(e -> e.getValue() == Severity.ERROR)
          .forEach(e -> e.setValue(Severity.WARN));
      this.dropErrorsToWarnings = dropErrorsToWarnings;
    }

    public void setDisableAllWarnings(boolean disableAllWarnings) {
      severityMap.entrySet().stream()
          .filter(e -> e.getValue() == Severity.WARN)
          .forEach(e -> e.setValue(Severity.OFF));
      this.disableAllWarnings = disableAllWarnings;
    }

    public void setEnableAllChecksAsWarnings(boolean enableAllChecksAsWarnings) {
      // Checks manually disabled before this flag are reset to warning-level
      severityMap.entrySet().stream()
          .filter(e -> e.getValue() == Severity.OFF)
          .forEach(e -> e.setValue(Severity.WARN));
      this.enableAllChecksAsWarnings = enableAllChecksAsWarnings;
    }

    public void setIgnoreLargeCodeGenerators(boolean ignoreLargeCodeGenerators) {
      this.ignoreLargeCodeGenerators = ignoreLargeCodeGenerators;
    }

    public void setDisableAllChecks(boolean disableAllChecks) {
      // Discard previously set severities so that the DisableAllChecks flag is position sensitive.
      severityMap.clear();
      this.disableAllChecks = disableAllChecks;
    }

    public void setTestOnlyTarget(boolean isTestOnlyTarget) {
      this.isTestOnlyTarget = isTestOnlyTarget;
    }

    public PatchingOptions.Builder patchingOptionsBuilder() {
      return patchingOptionsBuilder;
    }

    public ErrorProneOptions build(ImmutableList<String> remainingArgs) {
      return new ErrorProneOptions(
          ImmutableMap.copyOf(severityMap),
          remainingArgs,
          ignoreUnknownChecks,
          disableWarningsInGeneratedCode,
          disableAllWarnings,
          dropErrorsToWarnings,
          enableAllChecksAsWarnings,
          disableAllChecks,
          isTestOnlyTarget,
          flagsBuilder.build(),
          patchingOptionsBuilder.build(),
          excludedPattern,
          ignoreSuppressionAnnotations,
          ignoreLargeCodeGenerators);
    }

    public void setExcludedPattern(Pattern excludedPattern) {
      this.excludedPattern = excludedPattern;
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
    ImmutableList.Builder<String> remainingArgs = ImmutableList.builder();

    /* By default, we throw an error when an unknown option is passed in, if for example you
     * try to disable a check that doesn't match any of the known checks.  This catches typos from
     * the command line.
     *
     * You can pass the IGNORE_UNKNOWN_CHECKS_FLAG to opt-out of that checking.  This allows you to
     * use command lines from different versions of error-prone interchangeably.
     */
    Builder builder = new Builder();
    for (String arg : args) {
      switch (arg) {
        case IGNORE_SUPPRESSION_ANNOTATIONS:
          builder.setIgnoreSuppressionAnnotations(true);
          break;
        case IGNORE_UNKNOWN_CHECKS_FLAG:
          builder.setIgnoreUnknownChecks(true);
          break;
        case DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG:
          builder.setDisableWarningsInGeneratedCode(true);
          break;
        case ERRORS_AS_WARNINGS_FLAG:
          builder.setDropErrorsToWarnings(true);
          break;
        case ENABLE_ALL_CHECKS:
          builder.setEnableAllChecksAsWarnings(true);
          break;
        case DISABLE_ALL_CHECKS:
          builder.setDisableAllChecks(true);
          break;
        case COMPILING_TEST_ONLY_CODE:
          builder.setTestOnlyTarget(true);
          break;
        case DISABLE_ALL_WARNINGS:
          builder.setDisableAllWarnings(true);
          break;
        default:
          if (arg.startsWith(SEVERITY_PREFIX)) {
            builder.parseSeverity(arg);
          } else if (arg.startsWith(ErrorProneFlags.PREFIX)) {
            builder.parseFlag(arg);
          } else if (arg.startsWith(PATCH_OUTPUT_LOCATION)) {
            String remaining = arg.substring(PATCH_OUTPUT_LOCATION.length());
            if (remaining.equals("IN_PLACE")) {
              builder.patchingOptionsBuilder().inPlace(true);
            } else {
              if (remaining.isEmpty()) {
                throw new InvalidCommandLineOptionException("invalid flag: " + arg);
              }
              builder.patchingOptionsBuilder().baseDirectory(remaining);
            }
          } else if (arg.startsWith(PATCH_CHECKS_PREFIX)) {
            String remaining = arg.substring(PATCH_CHECKS_PREFIX.length());
            if (remaining.startsWith("refaster:")) {
              // Refaster rule, load from InputStream at file
              builder
                  .patchingOptionsBuilder()
                  .customRefactorer(
                      () -> {
                        String path = remaining.substring("refaster:".length());
                        try (InputStream in =
                                Files.newInputStream(FileSystems.getDefault().getPath(path));
                            ObjectInputStream ois = new ObjectInputStream(in)) {
                          return (CodeTransformer) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                          throw new RuntimeException("Can't load Refaster rule from " + path, e);
                        }
                      });
            } else {
              Iterable<String> checks = Splitter.on(',').trimResults().split(remaining);
              builder.patchingOptionsBuilder().namedCheckers(ImmutableSet.copyOf(checks));
            }
          } else if (arg.startsWith(PATCH_IMPORT_ORDER_PREFIX)) {
            String remaining = arg.substring(PATCH_IMPORT_ORDER_PREFIX.length());
            ImportOrganizer importOrganizer = ImportOrderParser.getImportOrganizer(remaining);
            builder.patchingOptionsBuilder().importOrganizer(importOrganizer);
          } else if (arg.startsWith(EXCLUDED_PATHS_PREFIX)) {
            String pathRegex = arg.substring(EXCLUDED_PATHS_PREFIX.length());
            builder.setExcludedPattern(Pattern.compile(pathRegex));

          } else {
            if (arg.startsWith(PREFIX)) {
              throw new InvalidCommandLineOptionException("invalid flag: " + arg);
            }
            remainingArgs.add(arg);
          }
      }
    }

    return builder.build(remainingArgs.build());
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
