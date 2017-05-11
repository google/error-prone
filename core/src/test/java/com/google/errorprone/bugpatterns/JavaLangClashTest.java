/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** {@link JavaLangClash}Test */
@RunWith(JUnit4.class)
public class JavaLangClashTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(JavaLangClash.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "foo/String.java", //
            "package foo;",
            "// BUG: Diagnostic contains:",
            "public class String {}")
        .doTest();
  }

  @Test
  public void positiveTypeParameter() {
    testHelper
        .addSourceLines(
            "java/lang/Foo.java", //
            "package java.lang;",
            "// BUG: Diagnostic contains:",
            "public class Foo<String> {}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "java/lang/String.java", //
            "package java.lang;",
            "public class String {}")
        .doTest();
  }

  @Test
  public void negativeNonPublic() {
    testHelper
        .addSourceLines(
            "test/AssertionStatusDirectives.java", //
            "package Test;",
            "public class AssertionStatusDirectives {}")
        .doTest();
  }
}
