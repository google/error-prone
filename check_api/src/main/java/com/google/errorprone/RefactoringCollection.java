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

package com.google.errorprone;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.auto.value.AutoValue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.errorprone.ErrorProneOptions.PatchingOptions;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.DiffApplier;
import com.google.errorprone.apply.FileDestination;
import com.google.errorprone.apply.FileSource;
import com.google.errorprone.apply.FsFileDestination;
import com.google.errorprone.apply.FsFileSource;
import com.google.errorprone.apply.ImportOrganizer;
import com.google.errorprone.apply.PatchFileDestination;
import com.google.errorprone.matchers.Description;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/** A container of fixes that have been collected during a single compilation phase. */
class RefactoringCollection implements DescriptionListener.Factory {
  private final Multimap<URI, DelegatingDescriptionListener> foundSources = HashMultimap.create();
  private final AtomicBoolean foundMatches = new AtomicBoolean(false);
  private final Path rootPath;
  private final FileDestination fileDestination;
  private final Callable<RefactoringResult> postProcess;
  private final DescriptionListener.Factory descriptionsFactory;
  private final ImportOrganizer importOrganizer;

  @AutoValue
  abstract static class RefactoringResult {
    abstract String message();

    abstract RefactoringResultType type();

    private static RefactoringResult create(String message, RefactoringResultType type) {
      return new AutoValue_RefactoringCollection_RefactoringResult(message, type);
    }
  }

  enum RefactoringResultType {
    NO_CHANGES,
    CHANGED,
  }

  static RefactoringCollection refactor(PatchingOptions patchingOptions) {
    Path rootPath = buildRootPath();
    FileDestination fileDestination;
    Callable<RefactoringResult> postProcess;

    if (patchingOptions.inPlace()) {
      fileDestination = new FsFileDestination(rootPath);
      postProcess =
          () ->
              RefactoringResult.create(
                  "Refactoring changes were successfully applied, please check the refactored code "
                      + "and recompile.",
                  RefactoringResultType.CHANGED);
    } else {
      Path baseDir = rootPath.resolve(patchingOptions.baseDirectory());
      Path patchFilePath = baseDir.resolve("error-prone.patch");

      PatchFileDestination patchFileDestination = new PatchFileDestination(baseDir, rootPath);
      postProcess =
          () -> {
            try {
              writePatchFile(patchFileDestination, patchFilePath);
              return RefactoringResult.create(
                  "Changes were written to "
                      + patchFilePath
                      + ". Please inspect the file and apply with: "
                      + "patch -p0 -u -i error-prone.patch",
                  RefactoringResultType.CHANGED);
            } catch (IOException e) {
              throw new RuntimeException("Failed to emit patch file!", e);
            }
          };
      fileDestination = patchFileDestination;
    }

    ImportOrganizer importOrganizer = patchingOptions.importOrganizer();
    return new RefactoringCollection(rootPath, fileDestination, postProcess, importOrganizer);
  }

  private RefactoringCollection(
      Path rootPath,
      FileDestination fileDestination,
      Callable<RefactoringResult> postProcess,
      ImportOrganizer importOrganizer) {
    this.rootPath = rootPath;
    this.fileDestination = fileDestination;
    this.postProcess = postProcess;
    this.descriptionsFactory = JavacErrorDescriptionListener.providerForRefactoring();
    this.importOrganizer = importOrganizer;
  }

  private static Path buildRootPath() {
    Path root = Iterables.getFirst(FileSystems.getDefault().getRootDirectories(), null);
    if (root == null) {
      throw new RuntimeException("Can't find a root filesystem!");
    }
    return root;
  }

  @Override
  public DescriptionListener getDescriptionListener(Log log, JCCompilationUnit compilation) {
    URI sourceFile = compilation.getSourceFile().toUri();

    DelegatingDescriptionListener delegate =
        new DelegatingDescriptionListener(
            descriptionsFactory.getDescriptionListener(log, compilation),
            DescriptionBasedDiff.createIgnoringOverlaps(compilation, importOrganizer));
    foundSources.put(sourceFile, delegate);
    return delegate;
  }

  RefactoringResult applyChanges() throws Exception {
    if (!foundMatches.get()) {
      return RefactoringResult.create("", RefactoringResultType.NO_CHANGES);
    }

    doApplyProcess(fileDestination, new FsFileSource(rootPath));
    return postProcess.call();
  }

  private static void writePatchFile(PatchFileDestination fileDestination, Path patchFilePatch)
      throws IOException {
    String patchFile = fileDestination.patchFile();
    if (!patchFile.isEmpty()) {
      Files.write(patchFilePatch, patchFile.getBytes(UTF_8));
    }
  }

  private void doApplyProcess(FileDestination fileDestination, FileSource fileSource) {
    DiffApplier diffApplier = new DiffApplier(4, fileSource, fileDestination);
    diffApplier.startAsync().awaitRunning();

    List<Future<?>> futures = new ArrayList<>();
    for (DelegatingDescriptionListener listener : foundSources.values()) {
      futures.add(diffApplier.put(listener.base));
    }
    diffApplier.stopAsync().awaitTerminated();

    try {
      for (Future<?> future : futures) {
        future.get();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private final class DelegatingDescriptionListener implements DescriptionListener {
    final DescriptionBasedDiff base;
    final DescriptionListener listener;

    DelegatingDescriptionListener(DescriptionListener listener, DescriptionBasedDiff base) {
      this.listener = listener;
      this.base = base;
    }

    @Override
    public void onDescribed(Description description) {
      foundMatches.set(true);
      listener.onDescribed(description);
      base.onDescribed(description);
    }
  }
}
