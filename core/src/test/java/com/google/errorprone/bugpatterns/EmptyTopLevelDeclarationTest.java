/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link EmptyTopLevelDeclaration}Test */
@RunWith(JUnit4.class)
public class EmptyTopLevelDeclarationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(EmptyTopLevelDeclaration.class, getClass());

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            """
            package a;

            class One {}
            """)
        .doTest();
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            """
            package a;

            class One {}
            // BUG: Diagnostic contains: remove
            ;
            """)
        .doTest();
  }

  // https://github.com/google/error-prone/issues/4245
  @Test
  public void noImports() {
    compilationHelper
        .addSourceLines(
            "ReproFile.java",
            """
            package errorpronecrash;
            // BUG: Diagnostic contains: Did you mean to remove this line?
            ;

            public class ReproFile {}
            """)
        .doTest();
  }
}
