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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HubSpotPatchUtils {
  private static final String FILE_NAME_FMT = "error-prone-%s.patch";

  public static Path resolvePatchFile(Path baseDir) {
    Path defaultPath = baseDir.resolve("error-prone.patch");
    if (Files.exists(defaultPath)) {
      int i = 1;
      Path curPath = getFileName(baseDir, i);
      while (Files.exists(curPath)) {
        i++;
        curPath = getFileName(baseDir, i);
      }

      return curPath;
    } else {
      return defaultPath;
    }
  }

  private static Path getFileName(Path baseDir, int id) {
    return baseDir.resolve(String.format(FILE_NAME_FMT, id));
  }

  private HubSpotPatchUtils() {
    throw new AssertionError() ;
  }
}
