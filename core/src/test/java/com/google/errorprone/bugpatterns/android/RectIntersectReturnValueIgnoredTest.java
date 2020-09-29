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

package com.google.errorprone.bugpatterns.android;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author avenet@google.com (Arnaud J. Venet) */
@RunWith(JUnit4.class)
public class RectIntersectReturnValueIgnoredTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RectIntersectReturnValueIgnored.class, getClass())
          .addSourceFile("testdata/stubs/android/graphics/Rect.java")
          .setArgs(ImmutableList.of("-XDandroidCompatible=true"));

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("RectIntersectReturnValueIgnoredPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("RectIntersectReturnValueIgnoredNegativeCases.java").doTest();
  }
}
