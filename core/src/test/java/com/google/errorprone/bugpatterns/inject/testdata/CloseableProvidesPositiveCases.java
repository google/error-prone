/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.testdata;

import com.google.inject.Provides;
import java.io.Closeable;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.inject.Singleton;

/** @author bhagwani@google.com (Sumit Bhagwani) */
public class CloseableProvidesPositiveCases {

  static class ImplementsClosable implements Closeable {
    public void close() {
      // no op
    }
  }

  @Provides
  // BUG: Diagnostic contains: CloseableProvides
  ImplementsClosable providesImplementsClosable() {
    return new ImplementsClosable();
  }

  @Provides
  @Singleton
  // BUG: Diagnostic contains: CloseableProvides
  PrintWriter providesPrintWriter() throws Exception {
    return new PrintWriter("some_file_path", StandardCharsets.UTF_8.name());
  }
}
