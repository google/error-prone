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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

/** Factories for in-memory {@link JavaFileObject}s, for testing. */
public final class FileObjects {

  /** Loads resources of the provided class into {@link JavaFileObject}s. */
  public static ImmutableList<JavaFileObject> forResources(Class<?> clazz, String... fileNames) {
    return stream(fileNames)
        .map(fileName -> forResource(clazz, fileName))
        .collect(toImmutableList());
  }

  /** Loads a resource of the provided class into a {@link JavaFileObject}. */
  public static JavaFileObject forResource(Class<?> clazz, String resourceName) {
    URI uri =
        URI.create(
            "file:///" + clazz.getPackage().getName().replace('.', '/') + "/" + resourceName);
    String content;
    try {
      content = new String(ByteStreams.toByteArray(findResource(clazz, resourceName)), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new SimpleJavaFileObject(uri, Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return content;
      }
    };
  }

  // TODO(b/176096448): the testdata/ fallback is a hack, fix affected tests and remove it
  private static InputStream findResource(Class<?> clazz, String name) {
    InputStream is = clazz.getResourceAsStream(name);
    if (is != null) {
      return is;
    }
    is = clazz.getResourceAsStream("testdata/" + name);
    if (is != null) {
      return is;
    }
    throw new AssertionError("could not find resource: " + name + " for: " + clazz);
  }

  /** Creates a {@link JavaFileObject} with the given name and content. */
  public static JavaFileObject forSourceLines(String path, String... lines) {
    return new SimpleJavaFileObject(URI.create("file:///" + path), Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return Joiner.on('\n').join(lines) + "\n";
      }
    };
  }

  private FileObjects() {}
}
