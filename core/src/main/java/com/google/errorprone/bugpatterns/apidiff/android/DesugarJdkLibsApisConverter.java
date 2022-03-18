/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.apidiff.android;

import com.google.errorprone.bugpatterns.apidiff.DesugarJdkLibsProto.DesugarJdkLibsMembers;
import com.google.errorprone.bugpatterns.apidiff.DesugarJdkLibsProto.MemberTracker;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Converts desugar_jdk_libs APIs from CSV to binary protos. */
public final class DesugarJdkLibsApisConverter {
  public static void main(String[] args) throws IOException {
    Path inputPath = Paths.get(args[0]);
    List<String> lines = Files.readAllLines(inputPath);
    int n = lines.size();
    DesugarJdkLibsMembers.Builder classMembers = DesugarJdkLibsMembers.newBuilder();
    for (int i = 1; i < n; i++) {
      classMembers.addMembers(
          MemberTracker.newBuilder()
              .setOwner(lines.get(1))
              .setName(lines.get(2))
              .setDesc(lines.get(3))
              .setOriginalOwner(lines.get(4))
              .setOriginalName(lines.get(5))
              .setOriginalDesc(lines.get(6))
              .build());
    }

    DesugarJdkLibsMembers desugarJdkLibsMembers = classMembers.build();

    Path outputPath =
        Files.createFile(
            Paths.get(
                "third_party/java_src/error_prone/project/core/src/main/java/com/google/errorprone/bugpatterns/apidiff/desugar_jdk_libs_apis.binarypb"));
    try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
      desugarJdkLibsMembers.writeTo(outputStream);
    }
  }

  private DesugarJdkLibsApisConverter() {}
}
