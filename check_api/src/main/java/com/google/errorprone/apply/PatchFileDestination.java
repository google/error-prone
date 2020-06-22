/*
 * Copyright 2016 The Error Prone Authors.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link FileDestination} that writes a unix-patch file to {@code rootPath} containing the
 * suggested changes.
 */
public final class PatchFileDestination implements FileDestination {

  // TODO(glorioso): This won't work for Windows, although getting unix patch on Windows is
  // a bit funky.
  private static final Splitter LINE_SPLITTER = Splitter.on('\n');

  private final Path baseDir;
  private final Path rootPath;
  // Path -> Unified Diff, sorted by path
  private final Map<URI, String> diffByFile = new TreeMap<>();

  public PatchFileDestination(Path baseDir, Path rootPath) {
    this.baseDir = baseDir;
    this.rootPath = rootPath;
  }

  @Override
  public void writeFile(SourceFile update) throws IOException {
    Path sourceFilePath = rootPath.resolve(update.getPath());
    String oldSource = new String(Files.readAllBytes(sourceFilePath), UTF_8);
    String newSource = update.getSourceText();
    if (!oldSource.equals(newSource)) {
      List<String> originalLines = LINE_SPLITTER.splitToList(oldSource);

      Patch<String> diff = null;
      try {
        diff = DiffUtils.diff(originalLines, LINE_SPLITTER.splitToList(newSource));
      } catch (DiffException e) {
        throw new AssertionError("DiffUtils.diff should not fail", e);
      }
      String relativePath = baseDir.relativize(sourceFilePath).toString();
      List<String> unifiedDiff =
          UnifiedDiffUtils.generateUnifiedDiff(relativePath, relativePath, originalLines, diff, 2);
      String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
      diffByFile.put(sourceFilePath.toUri(), diffString);
    }
  }

  public String patchFile(URI uri) {
    return diffByFile.remove(uri);
  }

  @Override
  public void flush() throws IOException {}
}
