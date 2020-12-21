/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.ThreadLocal.withInitial;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Jimfs;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.StandardLocation;

/**
 * Manages {@link JavacFileManager}s for use in Error Prone's test.
 *
 * <p>The file manager does some expensive IO to process the platform and user classpaths, so we
 * re-use the same filemanager instance across multiple tests to re-use that work.
 */
public final class FileManagers {

  // The file manager isn't thread-safe, so keep one instance per thread instead of using static
  // state, in case this is accessed from multiple threads.
  private static final ThreadLocal<JavacFileManager> FILE_MANAGER =
      withInitial(FileManagers::createFileManager);

  private static final ThreadLocal<FileSystem> FILE_SYSTEM = withInitial(Jimfs::newFileSystem);

  private static JavacFileManager createFileManager() {
    Context context = new Context();
    // Install the non-default caching version of FSInfo, which caches the result of filesystem
    // calls to files on the classpath.
    CacheFSInfo.preRegister(context);
    return new JavacFileManager(context, /* register= */ false, UTF_8);
  }

  /** Returns a {@link JavacFileManager} for use in compiler-based tests. */
  public static JavacFileManager testFileManager() {
    JavacFileManager fileManager = FILE_MANAGER.get();

    // Explicitly set the class path to the ambient runtime's classpath. This is the default
    // behaviour, but re-doing it for each test avoids issues when tests are executed in different
    // classloaders observed with IntelliJ and maven.
    setLocation(fileManager, systemClassPath(), StandardLocation.CLASS_PATH);

    // Set the output directories (for compiled classes and generated sources) to an in-memory
    // temporary directory, to avoid successful compilations trying to write their output to
    // local disk wherever the test is executing.
    Path tempDirectory;
    try {
      tempDirectory =
          Files.createTempDirectory(FILE_SYSTEM.get().getRootDirectories().iterator().next(), "");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    stream(StandardLocation.values())
        .filter(StandardLocation::isOutputLocation)
        .forEach(
            outputLocation ->
                setLocation(fileManager, ImmutableList.of(tempDirectory), outputLocation));

    return fileManager;
  }

  /** Returns the current runtime's classpath. */
  private static ImmutableList<Path> systemClassPath() {
    return Splitter.on(File.pathSeparatorChar)
        .splitToStream(JAVA_CLASS_PATH.value())
        .map(Paths::get)
        .collect(toImmutableList());
  }

  private static void setLocation(
      JavacFileManager fileManager, ImmutableList<Path> collect, StandardLocation classPath) {
    // Calling `setLocationFromPaths` on trusted inputs should never fail, so rethrow the
    // specified `IOException` as unchecked so we can call it in lambdas.
    try {
      fileManager.setLocationFromPaths(classPath, collect);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private FileManagers() {}
}
