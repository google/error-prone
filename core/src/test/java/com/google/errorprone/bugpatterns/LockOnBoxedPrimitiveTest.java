/*
 * Copyright 2020 The Error Prone Authors.
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

/**
 * Tests for {@link LockOnBoxedPrimitive} bugpattern.
 */
@RunWith(JUnit4.class)
public class LockOnBoxedPrimitiveTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LockOnBoxedPrimitive.class, getClass());

  @Test
  public void detectsSynchronizedBoxedLocks() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Byte badByteLock;",
            "  private Short badShortLock;",
            "  private Integer badIntLock;",
            "  private Long badLongLock;",
            "  private Float badFloatLock;",
            "  private Double badDoubleLock;",
            "  private Boolean badBooleanLock;",
            "  private Character badCharLock;",
            "  private void test() {",
            bugOnSynchronizedBlock("badByteLock"),
            bugOnSynchronizedBlock("badShortLock"),
            bugOnSynchronizedBlock("badIntLock"),
            bugOnSynchronizedBlock("badLongLock"),
            bugOnSynchronizedBlock("badFloatLock"),
            bugOnSynchronizedBlock("badDoubleLock"),
            bugOnSynchronizedBlock("badBooleanLock"),
            bugOnSynchronizedBlock("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsSynchronizedBoxedLocks_encapsulated_primitiveInitializers() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object badByteLock = (byte)1;",
            "  private Object badShortLock = (short)1;",
            "  private Object badIntLock = 1;",
            "  private Object badLongLock = 1L;",
            "  private Object badFloatLock = 1f;",
            "  private Object badDoubleLock = 1.0;",
            "  private Object badBooleanLock = false;",
            "  private Object badCharLock = 'c';",
            "  private void test() {",
            bugOnSynchronizedBlock("badByteLock"),
            bugOnSynchronizedBlock("badShortLock"),
            bugOnSynchronizedBlock("badIntLock"),
            bugOnSynchronizedBlock("badLongLock"),
            bugOnSynchronizedBlock("badFloatLock"),
            bugOnSynchronizedBlock("badDoubleLock"),
            bugOnSynchronizedBlock("badBooleanLock"),
            bugOnSynchronizedBlock("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsSynchronizedBoxedLocks_encapsulated_boxedInitializers() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object badByteLock = Byte.valueOf((byte)1);",
            "  private Object badShortLock = Short.valueOf((short)1);",
            "  private Object badIntLock = Integer.valueOf(1);",
            "  private Object badLongLock = Long.valueOf(1);",
            "  private Object badFloatLock = Float.valueOf(1.0f);",
            "  private Object badDoubleLock = Double.valueOf(1.0);",
            "  private Object badBooleanLock = Boolean.valueOf(false);",
            "  private Object badCharLock = Character.valueOf('c');",
            "  private void test() {",
            bugOnSynchronizedBlock("badByteLock"),
            bugOnSynchronizedBlock("badShortLock"),
            bugOnSynchronizedBlock("badIntLock"),
            bugOnSynchronizedBlock("badLongLock"),
            bugOnSynchronizedBlock("badFloatLock"),
            bugOnSynchronizedBlock("badDoubleLock"),
            bugOnSynchronizedBlock("badBooleanLock"),
            bugOnSynchronizedBlock("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsSynchronizedBoxedLocks_encapsulated_initializedLater() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object badByteLock;",
            "  private Object badShortLock;",
            "  private Object badIntLock;",
            "  private Object badLongLock;",
            "  private Object badFloatLock;",
            "  private Object badDoubleLock;",
            "  private Object badBooleanLock;",
            "  private Object badCharLock;",
            "  {",
            "    badByteLock = (byte)1;",
            "    badShortLock = (short)1;",
            "    badLongLock = 1L;",
            "    badIntLock = 1;",
            "    badFloatLock = 1f;",
            "    badDoubleLock = 1.0;",
            "    badBooleanLock = false;",
            "    badCharLock = 'c';",
            "  }",
            "  private void test() {",
            bugOnSynchronizedBlock("badByteLock"),
            bugOnSynchronizedBlock("badShortLock"),
            bugOnSynchronizedBlock("badIntLock"),
            bugOnSynchronizedBlock("badLongLock"),
            bugOnSynchronizedBlock("badFloatLock"),
            bugOnSynchronizedBlock("badDoubleLock"),
            bugOnSynchronizedBlock("badBooleanLock"),
            bugOnSynchronizedBlock("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsSynchronizedBoxedLocks_nonObjectPrimitiveParent() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Number badNumberLock = 3;",
            "  private void test() {",
            bugOnSynchronizedBlock("badNumberLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoresSynchronizedObjectLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object okLock;",
            "  private void test() {",
            "    synchronized (okLock) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoresSynchronizedObjectLock_initialized() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private final Object okLock = Test.class;",
            "  private void test() {",
            "    synchronized (okLock) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsMonitorMethodBoxedLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Byte badByteLock;",
            "  private Short badShortLock;",
            "  private Integer badIntLock;",
            "  private Long badLongLock;",
            "  private Float badFloatLock;",
            "  private Double badDoubleLock;",
            "  private Boolean badBooleanLock;",
            "  private Character badCharLock;",
            "  private void test() throws InterruptedException {",
            bugOnMonitorMethods("badByteLock"),
            bugOnMonitorMethods("badShortLock"),
            bugOnMonitorMethods("badIntLock"),
            bugOnMonitorMethods("badLongLock"),
            bugOnMonitorMethods("badFloatLock"),
            bugOnMonitorMethods("badDoubleLock"),
            bugOnMonitorMethods("badBooleanLock"),
            bugOnMonitorMethods("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsMonitorMethodBoxedLock_encapsulated_primitiveInitializers() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object badByteLock = (byte)1;",
            "  private Object badShortLock = (short)1;",
            "  private Object badIntLock = 1;",
            "  private Object badLongLock = 1L;",
            "  private Object badFloatLock = 1f;",
            "  private Object badDoubleLock = 1.0;",
            "  private Object badBooleanLock = false;",
            "  private Object badCharLock = 'c';",
            "  private void test() throws InterruptedException {",
            bugOnMonitorMethods("badByteLock"),
            bugOnMonitorMethods("badShortLock"),
            bugOnMonitorMethods("badIntLock"),
            bugOnMonitorMethods("badLongLock"),
            bugOnMonitorMethods("badFloatLock"),
            bugOnMonitorMethods("badDoubleLock"),
            bugOnMonitorMethods("badBooleanLock"),
            bugOnMonitorMethods("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsMonitorMethodBoxedLock_encapsulated_boxedInitializers() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object badByteLock = Byte.valueOf((byte)1);",
            "  private Object badShortLock = Short.valueOf((short)1);",
            "  private Object badIntLock = Integer.valueOf(1);",
            "  private Object badLongLock = Long.valueOf(1);",
            "  private Object badFloatLock = Float.valueOf(1.0f);",
            "  private Object badDoubleLock = Double.valueOf(1.0);",
            "  private Object badBooleanLock = Boolean.valueOf(false);",
            "  private Object badCharLock = Character.valueOf('c');",
            "  private void test() throws InterruptedException {",
            bugOnMonitorMethods("badByteLock"),
            bugOnMonitorMethods("badShortLock"),
            bugOnMonitorMethods("badIntLock"),
            bugOnMonitorMethods("badLongLock"),
            bugOnMonitorMethods("badFloatLock"),
            bugOnMonitorMethods("badDoubleLock"),
            bugOnMonitorMethods("badBooleanLock"),
            bugOnMonitorMethods("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void detectsMonitorMethodBoxedLock_encapsulated_initializedLater() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object badByteLock;",
            "  private Object badShortLock;",
            "  private Object badIntLock;",
            "  private Object badLongLock;",
            "  private Object badFloatLock;",
            "  private Object badDoubleLock;",
            "  private Object badBooleanLock;",
            "  private Object badCharLock;",
            "  {",
            "    badByteLock = (byte)1;",
            "    badShortLock = (short)1;",
            "    badIntLock = 1;",
            "    badLongLock = 1L;",
            "    badFloatLock = 1f;",
            "    badDoubleLock = 1.0;",
            "    badBooleanLock = false;",
            "    badCharLock = 'c';",
            "  }",
            "  private void test() throws InterruptedException {",
            bugOnMonitorMethods("badByteLock"),
            bugOnMonitorMethods("badShortLock"),
            bugOnMonitorMethods("badIntLock"),
            bugOnMonitorMethods("badLongLock"),
            bugOnMonitorMethods("badFloatLock"),
            bugOnMonitorMethods("badDoubleLock"),
            bugOnMonitorMethods("badBooleanLock"),
            bugOnMonitorMethods("badCharLock"),
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoresMonitorMethodObjectLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object okLock;",
            "  private void test() throws InterruptedException {",
            "    okLock.wait();",
            "    okLock.wait(1);",
            "    okLock.wait(1, 2);",
            "    okLock.notify();",
            "    okLock.notifyAll();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoresMonitorMethodObjectLock_initialized() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private final Object okLock = new Object();",
            "  private void test() throws InterruptedException {",
            "    okLock.wait();",
            "    okLock.wait(1);",
            "    okLock.wait(1, 2);",
            "    okLock.notify();",
            "    okLock.notifyAll();",
            "  }",
            "}")
        .doTest();
  }

  private static String bugOnSynchronizedBlock(String variableName) {
    String formatString =
        String.join(
            "\n",
            "    // BUG: Diagnostic contains: It is dangerous to use a boxed primitive as a lock",
            "    synchronized (%s) {",
            "    }");
    return String.format(formatString, variableName);
  }

  private static String bugOnMonitorMethods(String variableName) {
    String formatString =
        String.join(
            "\n",
            "    // BUG: Diagnostic contains: It is dangerous to use a boxed primitive as a lock",
            "    %s.wait();",
            "    // BUG: Diagnostic contains: It is dangerous to use a boxed primitive as a lock",
            "    %<s.wait(1);",
            "    // BUG: Diagnostic contains: It is dangerous to use a boxed primitive as a lock",
            "    %<s.wait(1, 2);",
            "    // BUG: Diagnostic contains: It is dangerous to use a boxed primitive as a lock",
            "    %<s.notify();",
            "    // BUG: Diagnostic contains: It is dangerous to use a boxed primitive as a lock",
            "    %<s.notifyAll();");
    return String.format(formatString, variableName);
  }
}
