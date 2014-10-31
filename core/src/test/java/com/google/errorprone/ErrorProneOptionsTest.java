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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.ErrorProneOptions.Severity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

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
    assertThat(options.getRemainingArgs(), equalTo(args));
  }

  @Test
  public void parsesDisableChecksFlag() {
    String[] args = {"-Xepdisable:foo,bar,baz"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    Map<String, Severity> expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("foo", Severity.OFF)
        .put("bar", Severity.OFF)
        .put("baz", Severity.OFF)
        .build();
    assertThat(options.getSeverityMap(), equalTo(expectedSeverityMap));
  }

  @Test
  public void combineErrorProneFlagsWithNonErrorProneFlags() {
    String[] args = {
        "-classpath", "/this/is/classpath",
        "-verbose",
        "-Xepdisable:foo,bar,baz"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    String[] expectedRemainingArgs = {"-classpath", "/this/is/classpath", "-verbose"};
    assertThat(options.getRemainingArgs(), equalTo(expectedRemainingArgs));
    Map<String, Severity> expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("foo", Severity.OFF)
        .put("bar", Severity.OFF)
        .put("baz", Severity.OFF)
        .build();
    assertThat(options.getSeverityMap(), equalTo(expectedSeverityMap));
  }

  @Test
  public void lastDisableChecksFlagWins() {
    String[] args = {
        "-Xepdisable:foo,bar,baz",
        "-classpath", "/this/is/classpath",
        "-verbose",
        "-Xepdisable:one,two,three"};
    ErrorProneOptions options = ErrorProneOptions.processArgs(args);
    Map<String, Severity> expectedSeverityMap = ImmutableMap.<String, Severity>builder()
        .put("one", Severity.OFF)
        .put("two", Severity.OFF)
        .put("three", Severity.OFF)
        .build();
    assertThat(options.getSeverityMap(), equalTo(expectedSeverityMap));
  }
}
