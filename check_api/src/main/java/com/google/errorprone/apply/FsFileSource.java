/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.apply;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** A FileSource that reads source files from the local filesystem. */
public final class FsFileSource implements FileSource {

  private final Path rootPath;

  public FsFileSource(Path rootPath) {
    this.rootPath = rootPath;
  }

  @Override
  public SourceFile readFile(String path) throws IOException {
    return new SourceFile(
        path, new String(Files.readAllBytes(rootPath.resolve(path)), StandardCharsets.UTF_8));
  }
}
