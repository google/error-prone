/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableSetMultimap;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Common {@link AutoCloseable} decorator types that wrap around {@link AutoCloseable} resources,
 * which are always closed when the enclosing decorators are closed.
 *
 * <p>Each value in the map is the common supertype of some {@link AutoCloseable} decorator types,
 * with the key being the common {@link AutoCloseable} type that they decorate. The decorated type
 * is always assumed to be the first argument to the constructor of the decorator type.
 */
public final class CloseableDecoratorTypes {
  public static final ImmutableSetMultimap<String, String> CLOSEABLE_DECORATOR_TYPES =
      ImmutableSetMultimap.<String, String>builder()
          .putAll(
              InputStream.class.getName(),
              FilterInputStream.class.getName(), // e.g., BufferedInputStream
              InputStreamReader.class.getName())
          .putAll(
              OutputStream.class.getName(),
              FilterOutputStream.class.getName(), // e.g., BufferedOutputStream
              OutputStreamWriter.class.getName())
          .put(Reader.class.getName(), Reader.class.getName()) // e.g., BufferedReader
          .put(Writer.class.getName(), Writer.class.getName()) // e.g., BufferedWriter
          .build();

  private CloseableDecoratorTypes() {}
}
