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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains options specific to error-prone.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ErrorProneOptions {

  private static final String DISABLE_FLAG_PREFIX = "-Xepdisable:";

  private Set<String> disabledChecks;
  private List<String> remainingArgs;

  private ErrorProneOptions(Set<String> disabledChecks, List<String>remainingArgs) {
    this.disabledChecks = disabledChecks;
    this.remainingArgs = remainingArgs;
  }

  public Set<String> getDisabledChecks() {
    return disabledChecks;
  }

  public String[] getRemainingArgs() {
    return remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  /**
   * Given a list of command-line arguments, produce the corresponding ErrorProneOptions instance.
   * If multiple -Xepdisable flags are passed, the last one wins.
   */
  public static ErrorProneOptions processArgs(String[] args) {
    List<String> outputArgs = new ArrayList<String>(args.length);
    Set<String> disabledChecks = Collections.emptySet();
    for (String arg : args) {
      if (arg.startsWith(DISABLE_FLAG_PREFIX)) {
        String checksToDisable = arg.substring(DISABLE_FLAG_PREFIX.length());
        disabledChecks = new HashSet<String>(Arrays.asList(checksToDisable.split(",")));
      } else {
        outputArgs.add(arg);
      }
    }
    return new ErrorProneOptions(disabledChecks, outputArgs);
  }
}
