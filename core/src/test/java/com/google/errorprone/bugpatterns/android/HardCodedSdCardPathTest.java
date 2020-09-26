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

package com.google.errorprone.bugpatterns.android;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author avenet@google.com (Arnaud J. Venet) */
@RunWith(JUnit4.class)
public class HardCodedSdCardPathTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(HardCodedSdCardPath.class, getClass());

  @Test
  public void matchingCode_onAndroid() {
    compilationHelper
        .setArgs(ImmutableList.of("-XDandroidCompatible=true"))
        .addSourceFile("HardCodedSdCardPathPositiveCases.java")
        .doTest();
  }

  @Test
  public void matchingCode_notOnAndroid() {
    compilationHelper
        .setArgs(ImmutableList.of("-XDandroidCompatible=false"))
        .addSourceLines(
            "HardCodedSdCardPathMatchingCode.java",
            "public class HardCodedSdCardPathMatchingCode {",
            "  static final String PATH1 = \"/sdcard\";",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .setArgs(ImmutableList.of("-XDandroidCompatible=true"))
        .addSourceFile("HardCodedSdCardPathNegativeCases.java")
        .doTest();
  }
}
