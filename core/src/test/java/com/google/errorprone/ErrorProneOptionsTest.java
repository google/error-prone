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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.ErrorProneOptions.Severity;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
  public void nonErrorProneFlagsPlacedInRemainingArgs() throws Exception {
    String[] args = {"-nonErrorProneFlag", "value"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    assertThat(options.getRemainingArgs()).isEqualTo(args);
  }

  @Test
  public void malformedOptionThrowsProperException() throws Exception {
    List<String> badArgs = Arrays.asList(
        "-Xep:Foo:WARN:jfkdlsdf", // too many parts
        "-Xep:", // no check name
        "-Xep:Foo:FJDKFJSD"); // nonexistent severity level
    for (String arg : badArgs) {
      try {
        ErrorProneOptions.processArgs(Arrays.asList(arg));
        fail();
      } catch (InvalidCommandLineOptionException expected) {
        assertThat(expected.getMessage()).contains("invalid flag");
      }
    }
  }

  @Test
  public void handlesErrorProneFlags() throws Exception {
    String[] args1 = {"-Xep:Check1"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args1);
    Map<String, Severity> expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("Check1", Severity.DEFAULT)
        .build();
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);

    String[] args2 = {"-Xep:Check1", "-Xep:Check2:OFF", "-Xep:Check3:WARN"};
    options = ErrorProneOptions.processArgs(args2);
    expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("Check1", Severity.DEFAULT)
        .put("Check2", Severity.OFF)
        .put("Check3", Severity.WARN)
        .build();
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }

  @Test
  public void combineErrorProneFlagsWithNonErrorProneFlags() throws Exception {
    String[] args = {
        "-classpath", "/this/is/classpath",
        "-verbose",
        "-Xep:Check1:WARN",
        "-Xep:Check2:ERROR"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    String[] expectedRemainingArgs = {"-classpath", "/this/is/classpath", "-verbose"};
    assertThat(options.getRemainingArgs()).isEqualTo(expectedRemainingArgs);
    Map<String, Severity> expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("Check1", Severity.WARN)
        .put("Check2", Severity.ERROR)
        .build();
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }

  @Test
  public void lastCheckFlagWins() throws Exception {
    String[] args = {
        "-Xep:Check1:ERROR",
        "-Xep:Check1:OFF"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    Map<String, Severity> expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("Check1", Severity.OFF)
        .build();
    assertThat(options.getSeverityMap()).isEqualTo(expectedSeverityMap);
  }
}
