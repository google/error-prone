/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import com.sun.tools.javac.nio.JavacPathFileManager;
import com.sun.tools.javac.util.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * An in-memory file manager for testing that uses {@link JavacPathFileManager} and {@link Jimfs}.
 */
public class ErrorProneInMemoryFileManager extends JavacPathFileManager {
  
  private final FileSystem fileSystem;
  
  public ErrorProneInMemoryFileManager() {
    super(new Context(), false, UTF_8);
    this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    setDefaultFileSystem(fileSystem);
  }
  
  /**
   * Loads resources of the current class into {@link JavaFileObject}s.
   */
  public List<JavaFileObject> sources(Class<?> clazz, String... fileNames) {
    ImmutableList.Builder<JavaFileObject> result = ImmutableList.builder();
    for (String fileName : fileNames) {
      result.add(source(clazz, fileName));
    }
    return result.build();
  }
  
  /**
   * Loads a resource of the current class into a {@link JavaFileObject}.
   */
  public JavaFileObject source(Class<?> clazz, String fileName) {
    Path path = fileSystem.getPath("/", clazz.getPackage().getName().replace('.', '/'), fileName);
    try {
      Files.createDirectories(path.getParent());
      try (InputStream inputStream = clazz.getResourceAsStream(fileName)) {
        Files.copy(inputStream, path);
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return Iterables.getOnlyElement(getJavaFileObjects(path));
  }

  /**
   * Creates a {@link JavaFileObject} with the given name and content. 
   */
  public JavaFileObject forSourceLines(String fileName, String... lines) {
    if (!fileName.startsWith("/")) {
      fileName = "/" + fileName;
    }
    Path path = fileSystem.getPath(fileName);
    try {
      Files.createDirectories(path.getParent());
      Files.write(path, Arrays.asList(lines), UTF_8);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return Iterables.getOnlyElement(getJavaFileObjects(path));
  }
}
