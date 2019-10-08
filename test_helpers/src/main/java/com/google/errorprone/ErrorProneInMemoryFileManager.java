/*
 * Copyright 2014 The Error Prone Authors.
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
import static javax.tools.StandardLocation.SOURCE_PATH;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaFileObject;

/** An in-memory file manager for testing that uses {@link JavacFileManager} and {@link Jimfs}. */
public class ErrorProneInMemoryFileManager extends JavacFileManager {
  private final FileSystem fileSystem;
  private final Optional<Class<?>> clazz;

  /**
   * Constructs an ErrorProneInMemoryFileManager instance.
   *
   * <p>Instances constructed with this constructor may not use the {@link #forResource(String)}
   * method to create a JavaFileObject from a source file on disk. If you wish to use that method,
   * use the {@link #ErrorProneInMemoryFileManager(Class)} constructor instead.
   */
  public ErrorProneInMemoryFileManager() {
    this(Optional.<Class<?>>absent());
  }

  /**
   * Constructs an ErrorProneInMemoryFileManager instance, given a class that can be used to lookup
   * file resources, such as test inputs to compile.
   *
   * @param clazz the class to use to locate file resources
   */
  public ErrorProneInMemoryFileManager(Class<?> clazz) {
    this(Optional.<Class<?>>of(clazz));
  }

  private ErrorProneInMemoryFileManager(Optional<Class<?>> clazz) {
    super(new Context(), /* register= */ false, UTF_8);
    this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    this.clazz = clazz;
  }

  /** Loads resources of the provided class into {@link JavaFileObject}s. */
  public List<JavaFileObject> forResources(Class<?> clazz, String... fileNames) {
    ImmutableList.Builder<JavaFileObject> result = ImmutableList.builder();
    for (String fileName : fileNames) {
      result.add(forResource(clazz, fileName));
    }
    return result.build();
  }

  /** Loads a resource of the provided class into a {@link JavaFileObject}. */
  public JavaFileObject forResource(Class<?> clazz, String fileName) {
    Path path = fileSystem.getPath("/", clazz.getPackage().getName().replace('.', '/'), fileName);
    try (InputStream is = findResource(clazz, fileName)) {
      Files.createDirectories(path.getParent());
      Files.copy(is, path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return Iterables.getOnlyElement(getJavaFileObjects(path));
  }

  @Override
  public boolean hasLocation(Location location) {
    /*
     * Short-circuit the check that module-info.java is found under the sourcepath.
     *
     * Here's why this short-circuits it:
     * http://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/jdk.compiler/share/classes/com/sun/tools/javac/comp/Modules.java#l523
     *
     * Here's why we want to do so:
     *
     * To determine whether a module-info.java is found under the sourcepath, javac looks for the
     * sourcepath Path objects on the default filesystem (i.e., using Paths.get(...)):
     * http://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/jdk.compiler/share/classes/com/sun/tools/javac/file/JavacFileManager.java#l112
     *
     * With some reflection on javac internals, we can override it to use our fileSystem.get(...).
     * However, there's still a problem, as javac converts the Path objects to File objects to do
     * its work:
     * http://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/jdk.compiler/share/classes/com/sun/tools/javac/file/JavacFileManager.java#l940
     *
     * This doesn't work for custom file systems like jimfs. So javac will never find anything under
     * our sourcepath (unless we start writing files to disk, which we could, like in our
     * integration tests).
     *
     * Thus, we short-circuit the check entirely.
     */
    return location != SOURCE_PATH && super.hasLocation(location);
  }

  // TODO(cushon): the testdata/ fallback is a hack, fix affected tests and remove it
  private InputStream findResource(Class<?> clazz, String name) {
    InputStream is = clazz.getResourceAsStream(name);
    if (is != null) {
      return is;
    }
    is = clazz.getResourceAsStream("testdata/" + name);
    if (is != null) {
      return is;
    }
    throw new AssertionError("could not find resource: " + name);
  }

  /** Loads a resource of the class passed into the constructor into a {@link JavaFileObject}. */
  public JavaFileObject forResource(String fileName) {
    Preconditions.checkState(
        clazz.isPresent(), "clazz must be set if you want to add a source from a resource file");
    return forResource(clazz.get(), fileName);
  }

  private Path resolvePath(String fileName) {
    if (!fileName.startsWith("/")) {
      fileName = "/" + fileName;
    }
    return fileSystem.getPath(fileName);
  }

  /** Creates a {@link JavaFileObject} with the given name and content. */
  public JavaFileObject forSourceLines(String fileName, String... lines) {
    Path path = resolvePath(fileName);
    try {
      Files.createDirectories(path.getParent());
      if (Files.exists(path)) {
        throw new AssertionError(
            "Refusing to overwrite an existing file (all results from that file would be lost)");
      }
      Files.write(path, Joiner.on('\n').join(lines).getBytes(UTF_8));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return Iterables.getOnlyElement(getJavaFileObjects(path));
  }

  public boolean exists(String fileName) {
    return Files.exists(resolvePath(fileName));
  }

  public FileSystem fileSystem() {
    return fileSystem;
  }
}
