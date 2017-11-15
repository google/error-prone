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
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnsafeFinalization}Test */
@RunWith(JUnit4.class)
public class UnsafeFinalizationTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(UnsafeFinalization.class, getClass());

  @Test
  public void positive() {
    compilationTestHelper
        .addSourceLines(
            "MyAwesomeGame.java",
            "class MyAwesomeGame {",
            "  private long nativeResourcePtr;",
            "  private static native long doNativeInit();",
            "  private static native void cleanUpNativeResources(long nativeResourcePtr);",
            "  private static native void playAwesomeGame(long nativeResourcePtr);",
            "  public MyAwesomeGame() {",
            "    nativeResourcePtr = doNativeInit();",
            "  }",
            "  @Override",
            "  protected void finalize() throws Throwable {",
            "    cleanUpNativeResources(nativeResourcePtr);",
            "    nativeResourcePtr = 0;",
            "    super.finalize();",
            "  }",
            "  public void run() {",
            "    // BUG: Diagnostic contains:",
            "    playAwesomeGame(nativeResourcePtr);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_instance() {
    compilationTestHelper
        .addSourceLines(
            "MyAwesomeGame.java",
            "class MyAwesomeGame {",
            "  private static long nativeResourcePtr;",
            "  private native void playAwesomeGame(long nativeResourcePtr);",
            "  @Override protected void finalize() {}",
            "  public void run() {",
            "    playAwesomeGame(nativeResourcePtr);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonIntOrLong() {
    compilationTestHelper
        .addSourceLines(
            "MyAwesomeGame.java",
            "class MyAwesomeGame {",
            "  private static String nativeResourcePtr;",
            "  private static native void playAwesomeGame(String nativeResourcePtr);",
            "  @Override protected void finalize() {}",
            "  public static void run() {",
            "    playAwesomeGame(nativeResourcePtr);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonNative() {
    compilationTestHelper
        .addSourceLines(
            "MyAwesomeGame.java",
            "class MyAwesomeGame {",
            "  private static long nativeResourcePtr;",
            "  private static void playAwesomeGame(long nativeResourcePtr) {}",
            "  @Override protected void finalize() {}",
            "  public static void run() {",
            "    playAwesomeGame(nativeResourcePtr);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_noInstanceState() {
    compilationTestHelper
        .addSourceLines(
            "MyAwesomeGame.java",
            "class MyAwesomeGame {",
            "  private long nativeResourcePtr;",
            "  private static native long doNativeInit();",
            "  private static native void cleanUpNativeResources(long nativeResourcePtr);",
            "  private static native void playAwesomeGame();",
            "  public MyAwesomeGame() {",
            "    nativeResourcePtr = doNativeInit();",
            "  }",
            "  @Override",
            "  protected void finalize() throws Throwable {",
            "    cleanUpNativeResources(nativeResourcePtr);",
            "    nativeResourcePtr = 0;",
            "    super.finalize();",
            "  }",
            "  public void run() {",
            "    playAwesomeGame();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeFence() {
    if (isJdk8OrEarlier()) {
      return;
    }
    compilationTestHelper
        .addSourceLines(
            "MyAwesomeGame.java",
            "import java.lang.ref.Reference;",
            "class MyAwesomeGame {",
            "  private long nativeResourcePtr;",
            "  private static native long doNativeInit();",
            "  private static native void cleanUpNativeResources(long nativeResourcePtr);",
            "  private static native void playAwesomeGame(long nativeResourcePtr);",
            "  public MyAwesomeGame() {",
            "    nativeResourcePtr = doNativeInit();",
            "  }",
            "  @Override",
            "  protected void finalize() throws Throwable {",
            "    cleanUpNativeResources(nativeResourcePtr);",
            "    nativeResourcePtr = 0;",
            "    super.finalize();",
            "  }",
            "  public void run() {",
            "    try {",
            "      playAwesomeGame(nativeResourcePtr);",
            "    } finally {",
            "      Reference.reachabilityFence(this);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  static boolean isJdk8OrEarlier() {
    try {
      Method versionMethod = Runtime.class.getMethod("version");
      Object version = versionMethod.invoke(null);
      int majorVersion = (int) version.getClass().getMethod("major").invoke(version);
      return majorVersion <= 8;
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }
}
