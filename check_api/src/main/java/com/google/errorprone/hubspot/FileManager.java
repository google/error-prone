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
import com.sun.tools.javac.util.Context;

class FileManager {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String OVERWATCH_DIR_ENV_VAR = "MAVEN_PROJECTBASEDIR";
  private static final String BLAZAR_DIR_ENV_VAR = "VIEWABLE_BUILD_ARTIFACTS_DIR";

  public static synchronized FileManager instance(Context context) {
    FileManager instance = context.get(FileManager.class);

    if (instance == null) {
      instance = new FileManager(HubSpotUtils.getPhase(context));
      context.put(FileManager.class, instance);
    }

    return instance;
  }

  private final String phase;

  FileManager(String phase) {
    this.phase = phase;
  }

  public String getPhase() {
    return phase;
  }

  Optional<Path> getErrorOutputPath() {
    return getDataDir(OVERWATCH_DIR_ENV_VAR, "target/overwatch-metadata")
        .map(o -> o.resolve("error-prone-exceptions.json"));
  }

  Optional<Path> getTimingsOutputPath() {
    return getDataDir(BLAZAR_DIR_ENV_VAR, "error-prone")
        .map(o -> o.resolve("error-prone-timings.json"));
  }

  Optional<Path> getLifeCycleCanaryPath(String id) {
    return getDataDir(OVERWATCH_DIR_ENV_VAR, "target/overwatch-metadata")
        .map(o -> o.resolve(String.format("lifecycle-canary-%s.json", id)));
  }

  Optional<Path> getUncaughtExceptionPath() {
    return getDataDir(BLAZAR_DIR_ENV_VAR, "error-prone")
        .map(o -> o.resolve("error-prone-exception.log"));
  }

  void write(Object data, Path path) {
    try (OutputStream stream = Files.newOutputStream(path)) {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(stream, data);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to write errorprone metadata to %s", path)
      );
    }
  }

  private Optional<Path> getDataDir(String envVar, String pathToAppend) {
    String dir = System.getenv(envVar);
    if (Strings.isNullOrEmpty(dir)) {
      return Optional.empty();
    }

    Path res = Paths.get(dir).resolve(pathToAppend).resolve("by-phase").resolve(phase);
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
