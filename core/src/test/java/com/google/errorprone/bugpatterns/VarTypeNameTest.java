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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link VarTypeName}Test */
@RunWith(JUnit4.class)
public class VarTypeNameTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(VarTypeName.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "class Test<var> {",
            "// BUG: Diagnostic contains:",
            "  class var {}",
            "// BUG: Diagnostic contains:",
            "  public <var> void foo(var foo) {}",
            "}")
        .setArgs(ImmutableList.of("-source", "8", "-target", "8"))
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  int var;",
            "}")
        .setArgs(ImmutableList.of("-source", "8", "-target", "8"))
        .doTest();
  }
}
