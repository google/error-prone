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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.io.Resources;
import com.google.errorprone.BugPattern;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Checks for uses of classes, fields, or methods that are not compatible with JDK 7 */
@BugPattern(
    name = "Java7ApiChecker",
    summary = "Use of class, field, or method that is not compatible with JDK 7",
    explanation =
        "Code that needs to be compatible with Java 7 cannot use types or members"
            + " that are only present in the JDK 8 class libraries",
    category = JDK,
    severity = ERROR,
    suppressionAnnotations = {
      SuppressWarnings.class
    })
public class Java7ApiChecker extends ApiDiffChecker {

  static final ApiDiff API_DIFF = loadApiDiff();

  private static ApiDiff loadApiDiff() {
    try {
      ApiDiffProto.Diff.Builder diffBuilder = ApiDiffProto.Diff.newBuilder();
      byte[] diffData =
          Resources.toByteArray(Resources.getResource(Java7ApiChecker.class, "7to8diff.binarypb"));
      diffBuilder.mergeFrom(diffData);
      diffBuilder
          .addClassDiff(
              ApiDiffProto.ClassDiff.newBuilder()
                  .setMemberDiff(
                      ApiDiffProto.MemberDiff.newBuilder()
                          .setClassName("com/google/common/base/Predicate")
                          .addMember(
                              ApiDiffProto.ClassMember.newBuilder()
                                  .setIdentifier("test")
                                  .setMemberDescriptor("(Ljava/lang/Object;)Z"))))
          .addClassDiff(
              ApiDiffProto.ClassDiff.newBuilder()
                  .setMemberDiff(
                      ApiDiffProto.MemberDiff.newBuilder()
                          .setClassName("com/google/common/base/BinaryPredicate")
                          .addMember(
                              ApiDiffProto.ClassMember.newBuilder()
                                  .setIdentifier("test")
                                  .setMemberDescriptor(
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Z"))));
      return ApiDiff.fromProto(diffBuilder.build());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Java7ApiChecker() {
    super(
        API_DIFF
        );
  }
}
