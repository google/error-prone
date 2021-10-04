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

import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.Resources;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.bugpatterns.apidiff.ApiDiff.ClassMemberKey;
import com.google.protobuf.ExtensionRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.regex.Pattern;

/** Checks for uses of classes, fields, or methods that are not compatible with JDK 8 */
@BugPattern(
    name = "Java8ApiChecker",
    summary = "Use of class, field, or method that is not compatible with JDK 8",
    explanation =
        "Code that needs to be compatible with Java 8 cannot use types or members"
            + " that are only present in newer class libraries",
    severity = ERROR)
public class Java8ApiChecker extends ApiDiffChecker {

  private static ApiDiff loadApiDiff(ErrorProneFlags errorProneFlags) {
    try {
      byte[] diffData =
          Resources.toByteArray(Resources.getResource(Java8ApiChecker.class, "8to11diff.binarypb"));
      ApiDiff diff =
          ApiDiff.fromProto(
              ApiDiffProto.Diff.newBuilder()
                  .mergeFrom(diffData, ExtensionRegistry.getEmptyRegistry())
                  .build());
      boolean checkBuffer = errorProneFlags.getBoolean("Java8ApiChecker:checkBuffer").orElse(true);
      boolean checkChecksum =
          errorProneFlags.getBoolean("Java8ApiChecker:checkChecksum").orElse(true);
      if (checkBuffer && checkChecksum) {
        return diff;
      }
      ImmutableMultimap<String, ClassMemberKey> unsupportedMembers =
          diff.unsupportedMembersByClass().entries().stream()
              .filter(e -> checkBuffer || !BUFFER.matcher(e.getKey()).matches())
              .filter(e -> checkChecksum || !e.getKey().equals(CHECKSUM))
              .collect(toImmutableSetMultimap(Map.Entry::getKey, Map.Entry::getValue));
      return ApiDiff.fromMembers(diff.unsupportedClasses(), unsupportedMembers);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final Pattern BUFFER = Pattern.compile("java/nio/.*Buffer");

  private static final String CHECKSUM = "java/util/zip/Checksum";

  public Java8ApiChecker(ErrorProneFlags errorProneFlags) {
    super(loadApiDiff(errorProneFlags));
  }
}
