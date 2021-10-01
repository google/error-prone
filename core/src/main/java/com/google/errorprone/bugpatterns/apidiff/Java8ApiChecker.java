/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.apidiff;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.io.Resources;
import com.google.errorprone.BugPattern;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Checks for uses of classes, fields, or methods that are not compatible with JDK 8 */
@BugPattern(
    name = "Java8ApiChecker",
    summary = "Use of class, field, or method that is not compatible with JDK 8",
    explanation =
        "Code that needs to be compatible with Java 8 cannot use types or members"
            + " that are only present in newer class libraries",
    severity = ERROR)
public class Java8ApiChecker extends ApiDiffChecker {

  static final ApiDiff API_DIFF = loadApiDiff();

  private static ApiDiff loadApiDiff() {
    try {
      ApiDiffProto.Diff.Builder diffBuilder = ApiDiffProto.Diff.newBuilder();
      byte[] diffData =
          Resources.toByteArray(Resources.getResource(Java8ApiChecker.class, "8to11diff.binarypb"));
      diffBuilder.mergeFrom(diffData);
      return ApiDiff.fromProto(diffBuilder.build());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Java8ApiChecker() {
    super(API_DIFF);
  }
}
