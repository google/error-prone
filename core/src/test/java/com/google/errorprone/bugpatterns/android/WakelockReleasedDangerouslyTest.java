/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author epmjohnston@google.com */
@RunWith(JUnit4.class)
public class WakelockReleasedDangerouslyTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new WakelockReleasedDangerously(), getClass())
          .setArgs(ImmutableList.of("-XDandroidCompatible=true"))
          .addInput("testdata/stubs/android/os/PowerManager.java")
          .expectUnchanged();
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(WakelockReleasedDangerously.class, getClass())
          .addSourceFile("testdata/stubs/android/os/PowerManager.java")
          .setArgs(ImmutableList.of("-XDandroidCompatible=true"));

  @Test
  public void dangerousWakelockRelease_refactoring() {
    refactoringHelper
        .addInputLines(
            "in/TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    if (wakelock.isHeld()) {",
            "      doSomethingElse();",
            "      wakelock.release();",
            "",
            "    // Make sure comments are preserved",
            "    }",
            "  }",
            "  void doSomethingElse() {}",
            "}")
        .addOutputLines(
            "out/TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    doSomethingElse();",
            "    try {",
            "      wakelock.release();",
            "    } catch (RuntimeException unused) {",
            "      // Ignore: wakelock already released by timeout.",
            "      // TODO: Log this exception.",
            "    }",
            "",
            "    // Make sure comments are preserved",
            "  }",
            "  void doSomethingElse() {}",
            "}")
        .doTest();
    // TODO(b/33069946): use TestMode.TEXT_MATCH to check comment is preserved.
  }

  @Test
  public void doesNotRemoveIsHeldOnDifferentSymbol() {
    refactoringHelper
        .addInputLines(
            "in/TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wl1, WakeLock wl2) {",
            "    wl1.acquire(100);",
            "    if (wl2.isHeld()) {",
            "      wl1.release();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wl1, WakeLock wl2) {",
            "    wl1.acquire(100);",
            "    if (wl2.isHeld()) {",
            "      try {",
            "        wl1.release();",
            "      } catch (RuntimeException unused) {",
            "        // Ignore: wakelock already released by timeout.",
            "        // TODO: Log this exception.",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dangerousWakelockRelease_lambda_refactoring() {
    refactoringHelper
        .addInputLines(
            "in/TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    doThing(() -> wakelock.release());",
            "  }",
            "  void doThing(Runnable thing) {}",
            "}")
        .addOutputLines(
            "out/TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    doThing(() -> {",
            "        try {",
            "          wakelock.release();",
            "        } catch (RuntimeException unused) {",
            "          // Ignore: wakelock already released by timeout.",
            "          // TODO: Log this exception.",
            "        }",
            "    });",
            "  }",
            "  void doThing(Runnable thing) {}",
            "}")
        .doTest();
  }

  @Test
  public void acquiredWithoutTimeout_shouldBeOkay() {
    compilationHelper
        .addSourceLines(
            "WithoutTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithoutTimeout {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire();",
            "    wakelock.release();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchesRuntimeException_shouldBeOkay() {
    compilationHelper
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      wakelock.release();",
            "    } catch (RuntimeException e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noCatch_shouldWarn() {
    compilationHelper
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "import java.io.BufferedReader;",
            "import java.io.FileReader;",
            "import java.io.IOException;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) throws IOException {",
            "    wakelock.acquire(100);",
            // try-with-resources so catch block is optional.
            "    try (BufferedReader br = new BufferedReader(new FileReader(\"\"))) {",
            "      // BUG: Diagnostic contains: Wakelock",
            "      wakelock.release();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchesSuperclassOfRuntimeException_shouldBeOkay() {
    compilationHelper
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      wakelock.release();",
            "    } catch (Exception e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchesSubclassOfRuntimeException_shouldWarn() {
    compilationHelper
        .addSourceLines(
            "MyRuntimeException.java",
            "public class MyRuntimeException extends RuntimeException {}")
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      // BUG: Diagnostic contains: Wakelock",
            "      wakelock.release();",
            "    } catch (MyRuntimeException e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchesOtherException_shouldWarn() {
    compilationHelper
        .addSourceLines(
            "MyOtherException.java", "public class MyOtherException extends Exception {}")
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      // BUG: Diagnostic contains: Wakelock",
            "      wakelock.release();",
            "      throw new MyOtherException();",
            "    } catch (MyOtherException e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedCatch_shouldWarn() {
    compilationHelper
        .addSourceLines(
            "MyOtherException.java", "public class MyOtherException extends Exception {}")
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      try {",
            "        // BUG: Diagnostic contains: Wakelock",
            "        wakelock.release();",
            "        throw new MyOtherException();",
            "      } catch (MyOtherException e) {}",
            "    } catch (RuntimeException err) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchesUnion_withRuntimeException_shouldBeOkay() {
    compilationHelper
        .addSourceLines(
            "MyOtherException.java", "public class MyOtherException extends Exception {}")
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      wakelock.release();",
            "      throw new MyOtherException();",
            "    } catch (RuntimeException|MyOtherException e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void catchesUnion_withLeastUpperBoundException_shouldWarn() {
    compilationHelper
        .addSourceLines(
            "TestApp.java",
            "import android.os.PowerManager.WakeLock;",
            "import java.io.IOException;",
            "public class TestApp {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "    try {",
            "      // BUG: Diagnostic contains: Wakelock",
            "      wakelock.release();",
            "      throw new IOException();",
            "    } catch (IOException | NullPointerException e) {",
            "      // union with a 'least upper bound' of Exception, won't catch RuntimeException.",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void acquiredElsewhere_shouldBeRecognized() {
    compilationHelper
        .addSourceLines(
            "WithTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithTimeout {",
            "  WakeLock wakelock;",
            "  WithTimeout(WakeLock wl) {",
            "    this.wakelock = wl;",
            "    this.wakelock.acquire(100);",
            "  }",
            "  void foo() {",
            "    // BUG: Diagnostic contains: Wakelock",
            "    wakelock.release();",
            "  }",
            "}")
        .addSourceLines(
            "WithoutTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithoutTimeout {",
            "  WakeLock wakelock;",
            "  WithoutTimeout(WakeLock wl) {",
            "    this.wakelock = wl;",
            "    this.wakelock.acquire();",
            "  }",
            "  void foo() {",
            "    wakelock.release();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void differentWakelock_shouldNotBeRecognized() {
    compilationHelper
        .addSourceLines(
            "WithTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithTimeout {",
            "  void bar(WakeLock wakelock) {",
            "    wakelock.acquire(100);",
            "  }",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.release();",
            "  }",
            "}")
        .addSourceLines(
            "WithoutTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithoutTimeout {",
            "  void bar(WakeLock wakelock) {",
            "    wakelock.acquire();",
            "  }",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.release();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wakelockFromMethod() {
    compilationHelper
        .addSourceLines(
            "WithTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithTimeout {",
            "  WakeLock wakelock;",
            "  WakeLock getWakelock() { return wakelock; }",
            "  void bar() {",
            "    getWakelock().acquire(100);",
            "  }",
            "  void foo() {",
            "    // BUG: Diagnostic contains: Wakelock",
            "    getWakelock().release();",
            "  }",
            "}")
        .addSourceLines(
            "WithoutTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class WithoutTimeout {",
            "  WakeLock wakelock;",
            "  WakeLock getWakelock() { return wakelock; }",
            "  void bar(WakeLock wakelock) {",
            "    getWakelock().acquire();",
            "  }",
            "  void foo(WakeLock wakelock) {",
            "    getWakelock().release();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wakelockNotReferenceCounted_shouldBeOkay() {
    compilationHelper
        .addSourceLines(
            "NotReferenceCountedTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class NotReferenceCountedTimeout {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.setReferenceCounted(false);",
            "    wakelock.acquire(100);",
            "    wakelock.release();",
            "  }",
            "}")
        .addSourceLines(
            "ExplicitlyReferenceCountedTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class ExplicitlyReferenceCountedTimeout {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.setReferenceCounted(true);",
            "    wakelock.acquire(100);",
            "    // BUG: Diagnostic contains: Wakelock",
            "    wakelock.release();",
            "  }",
            "}")
        .addSourceLines(
            "NotReferenceCountedNoTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class NotReferenceCountedNoTimeout {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.setReferenceCounted(false);",
            "    wakelock.acquire();",
            "    wakelock.release();",
            "  }",
            "}")
        .addSourceLines(
            "ExplicitlyReferenceCountedNoTimeout.java",
            "import android.os.PowerManager.WakeLock;",
            "public class ExplicitlyReferenceCountedNoTimeout {",
            "  void foo(WakeLock wakelock) {",
            "    wakelock.setReferenceCounted(true);",
            "    wakelock.acquire();",
            "    wakelock.release();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void innerClass_negative() {
    compilationHelper
        .addSourceLines(
            "OuterClass.java",
            "import android.os.PowerManager.WakeLock;",
            "public class OuterClass {",
            "  WakeLock wakelock;",
            "  OuterClass(WakeLock wl) {",
            "    this.wakelock = wl;",
            "    this.wakelock.setReferenceCounted(false);",
            "  }",
            "  public class InnerClass {",
            "    void foo() {",
            "      wakelock.acquire(100);",
            "      wakelock.release();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void innerClass_positive() {
    compilationHelper
        .addSourceLines(
            "OuterClass.java",
            "import android.os.PowerManager.WakeLock;",
            "public class OuterClass {",
            "  WakeLock wakelock;",
            "  OuterClass(WakeLock wl) {",
            "    wakelock = wl;",
            "    wakelock.acquire(100);",
            "  }",
            "  public class InnerClass {",
            "    void foo() {",
            "      // BUG: Diagnostic contains: Wakelock",
            "      wakelock.release();",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
