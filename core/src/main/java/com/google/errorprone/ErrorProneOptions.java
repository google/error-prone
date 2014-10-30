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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;

/**
 * Contains options specific to error-prone.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ErrorProneOptions {

  private static final String DISABLE_FLAG_PREFIX = "-Xepdisable:";

  /**
   * Severity levels for an error-prone check that define how the check results should be
   * presented.
   */
  // TODO(user): Add support for other severity levels, e.g. DEFAULT, WARN, and ERROR.
  public enum Severity {
    OFF,
  }

  /**
   * see {@link javax.tools.OptionChecker#isSupportedOption(String)}
   */
  public static int isSupportedOption(String option) {
    return option.startsWith(DISABLE_FLAG_PREFIX) ? 0 : -1;
  }

  private final ImmutableList<String> remainingArgs;
  private final ImmutableMap<String, Severity> severityMap;

  private ErrorProneOptions(ImmutableMap<String, Severity> severityMap,
      ImmutableList<String> remainingArgs) {
    this.severityMap = severityMap;
    this.remainingArgs = remainingArgs;
  }

  public String[] getRemainingArgs() {
    return remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  public ImmutableMap<String, Severity> getSeverityMap() {
    return severityMap;
  }

  /**
   * Given a list of command-line arguments, produce the corresponding ErrorProneOptions instance.
   * If multiple -Xepdisable flags are passed, the last one wins.
   *
   * @param args compiler args, possibly {@code null}
   */
  public static ErrorProneOptions processArgs(Iterable<String> args) {
    ImmutableList.Builder<String> outputArgs = ImmutableList.builder();
    ImmutableMap.Builder<String, Severity> severityMap = ImmutableMap.builder();
    if (args != null) {
      for (String arg : args) {
        if (arg.startsWith(DISABLE_FLAG_PREFIX)) {
          String[] checksToDisable = arg.substring(DISABLE_FLAG_PREFIX.length()).split(",");
          severityMap = ImmutableMap.builder();
          for (String checkName : checksToDisable) {
            severityMap.put(checkName, Severity.OFF);
          }
        } else {
          outputArgs.add(arg);
        }
      }
    }
    return new ErrorProneOptions(severityMap.build(), outputArgs.build());
  }

  public static ErrorProneOptions processArgs(String[] args) {
    return processArgs(Arrays.asList(args));
  }
}
