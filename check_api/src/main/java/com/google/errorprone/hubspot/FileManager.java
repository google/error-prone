/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.hubspot;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

class FileManager {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String OVERWATCH_DIR_ENV_VAR = "MAVEN_PROJECTBASEDIR";
  private static final String BLAZAR_DIR_ENV_VAR = "VIEWABLE_BUILD_ARTIFACTS_DIR";

  static Optional<Path> getErrorOutputPath() {
    return getDataDir(OVERWATCH_DIR_ENV_VAR, "target/overwatch-metadata")
        .map(o -> o.resolve("error-prone-exceptions.json"));
  }

  static Optional<Path> getTimingsOutputPath() {
    return getDataDir(BLAZAR_DIR_ENV_VAR, "error-prone")
        .map(o -> o.resolve("error-prone-timings.json"));
  }

  static Optional<Path> getLifeCycleCanaryPath(String id) {
    return getDataDir(OVERWATCH_DIR_ENV_VAR, "target/overwatch-metadata")
        .map(o -> o.resolve(String.format("lifecycle-canary-%s.json", id)));
  }

  static void write(Object data, Path path) {
    try (OutputStream stream = Files.newOutputStream(path)) {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(stream, data);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to write errorprone metadata to %s", path)
      );
    }
  }

  private static Optional<Path> getDataDir(String envVar, String pathToAppend) {
    String dir = System.getenv(envVar);
    if (Strings.isNullOrEmpty(dir)) {
      return Optional.empty();
    }

    Path res = Paths.get(dir).resolve(pathToAppend);
    if (!Files.exists(res)) {
      try {
        Files.createDirectories(res);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to create directory: %s", res),
            e
        );
      }
    }

    return Optional.of(res);
  }
}
