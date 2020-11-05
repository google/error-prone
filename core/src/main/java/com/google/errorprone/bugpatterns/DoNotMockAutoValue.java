/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.auto.value.AutoValue;
import com.google.errorprone.BugPattern;
import com.sun.source.tree.VariableTree;
import java.util.stream.Stream;

/** Suggests not mocking AutoValue classes. */
@BugPattern(
    name = "DoNotMockAutoValue",
    summary =
        "AutoValue classes represent pure data classes, so mocking them should not be necessary."
            + " Construct a real instance of the class instead.",
    severity = WARNING)
public final class DoNotMockAutoValue extends AbstractMockChecker<AutoValue> {
  private static final TypeExtractor<VariableTree> MOCKED_VAR =
      fieldAnnotatedWithOneOf(
          Stream.of(
              "org.mockito.Mock",
              "org.mockito.Spy"));

  public DoNotMockAutoValue() {
    super(
        MOCKED_VAR,
        MOCKING_METHOD,
        AutoValue.class,
        unused ->
            "AutoValue classes represent pure value classes, so mocking them should not be"
                + " necessary");
  }
}
