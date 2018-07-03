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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * {@link DeprecatedThreadMethods}Test
 *
 * @author siyuanl@google.com (Siyuan Liu)
 */
@RunWith(JUnit4.class)
public final class DeprecatedThreadMethodsTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(DeprecatedThreadMethods.class, getClass());
  }

  @Test
  public void stopThread() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "    // BUG: Diagnostic contains: DeprecatedThreadMethods",
            "    thread.stop();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stopThrowableThread() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "    // BUG: Diagnostic contains: DeprecatedThreadMethods",
            "    thread.stop(new Throwable(\"lol pls throw\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void countStackFramesThread() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "    // BUG: Diagnostic contains: DeprecatedThreadMethods",
            "    thread.countStackFrames();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void destroyThread() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "    // BUG: Diagnostic contains: DeprecatedThreadMethods",
            "    thread.destroy();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void resumeThread() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "    // BUG: Diagnostic contains: DeprecatedThreadMethods",
            "    thread.resume();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suspendThread() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "    // BUG: Diagnostic contains: DeprecatedThreadMethods",
            "    thread.suspend();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread thread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    thread.start();",
            "  }",
            "}")
        .doTest();
  }
}
