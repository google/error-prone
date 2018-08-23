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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author epmjohnston@google.com (Emily P.M. Johnston) */
@RunWith(JUnit4.class)
public final class IsLoggableTagLengthTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(IsLoggableTagLength.class, getClass())
          .addSourceFile("testdata/stubs/android/util/Log.java");

  @Test
  public void testNegativeCaseLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  public void log() { Log.isLoggable(\"SHORT_ENOUGH\", Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCaseLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  // BUG: Diagnostic contains: IsLoggableTagLength",
            "  public void log() { Log.isLoggable(\"THIS_TAG_NAME_IS_WAY_TOO_LONG\", Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCaseLiteralUnicode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  public void log() { Log.isLoggable(\"🚀🚀🚀🚀\", Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCaseLiteralUnicode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  // BUG: Diagnostic contains: IsLoggableTagLength",
            "  public void log() { Log.isLoggable(\"☔☔☔☔☔☔☔☔☔☔☔☔\", Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCaseFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  static final String TAG = \"SHORT_ENOUGH\";",
            "  public void log() { Log.isLoggable(TAG, Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCaseFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  static final String TAG = \"THIS_TAG_NAME_IS_WAY_TOO_LONG\";",
            "  // BUG: Diagnostic contains: IsLoggableTagLength",
            "  public void log() { Log.isLoggable(TAG, Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCaseClassName() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  public void log() { Log.isLoggable(Test.class.getSimpleName(), Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCaseClassName() {
    compilationHelper
        .addSourceLines(
            "ThisClassNameIsWayTooLong.java",
            "import android.util.Log;",
            "class ThisClassNameIsWayTooLong {",
            "  public void log() {",
            "    // BUG: Diagnostic contains: IsLoggableTagLength",
            "    Log.isLoggable(ThisClassNameIsWayTooLong.class.getSimpleName(), Log.INFO);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCaseFinalFieldClassName() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  static final String TAG = Test.class.getSimpleName();",
            "  public void log() {",
            "    Log.isLoggable(TAG, Log.INFO);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCaseFinalFieldClassName() {
    compilationHelper
        .addSourceLines(
            "ThisClassNameIsWayTooLong.java",
            "import android.util.Log;",
            "class ThisClassNameIsWayTooLong {",
            "  static final String TAG = ThisClassNameIsWayTooLong.class.getSimpleName();",
            "  public void log() {",
            "    // BUG: Diagnostic contains: IsLoggableTagLength",
            "    Log.isLoggable(TAG, Log.INFO);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCaseNonFinalFieldUninitialized() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import android.util.Log;",
            "class Test {",
            "  String unknownValue;",
            "  public void log() { Log.isLoggable(unknownValue, Log.INFO); }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCaseNonFinalFieldClassNameTooLong() {
    compilationHelper
        .addSourceLines(
            "ThisClassNameIsWayTooLong.java",
            "import android.util.Log;",
            "class ThisClassNameIsWayTooLong {",
            "  String TAG = ThisClassNameIsWayTooLong.class.getSimpleName();",
            "  public void log() {",
            "    Log.isLoggable(TAG, Log.INFO);",
            "  }",
            "}")
        .doTest();
  }
}
