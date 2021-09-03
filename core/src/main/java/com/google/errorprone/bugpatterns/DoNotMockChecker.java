/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.annotations.DoNotMock;
import com.sun.source.tree.VariableTree;
import java.util.stream.Stream;

/**
 * Points out if a Mockito or EasyMock mock is mocking an object that would be better off being
 * tested using an alternative instance.
 *
 * @author amalloy@google.com (Alan Malloy)
 */
@BugPattern(
    name = "DoNotMock",
    severity = SeverityLevel.ERROR,
    summary = "Identifies undesirable mocks.",
    documentSuppression = false)
public class DoNotMockChecker extends AbstractMockChecker<DoNotMock> {

  private static final TypeExtractor<VariableTree> MOCKED_VAR =
      fieldAnnotatedWithOneOf(Stream.of("org.mockito.Mock", "org.mockito.Spy"));

  public DoNotMockChecker() {
    super(MOCKED_VAR, MOCKING_METHOD, DoNotMock.class, DoNotMock::value);
  }
}
