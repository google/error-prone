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
 * {@link ThreadPriorityCheck}Test
 *
 * @author siyuanl@google.com (Siyuan Liu)
 * @author eleanorh@google.com (Eleanor Harris)
 */
@RunWith(JUnit4.class)
public final class ThreadPriorityCheckTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ThreadPriorityCheck.class, getClass());
  }

  @Test
  public void yieldThread() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    Thread myThread = new Thread(new Runnable() {",
            "      @Override",
            "      public void run() {",
            "        System.out.println(\"Run, thread, run!\");",
            "      }",
            "    });",
            "    myThread.start();",
            "    // BUG: Diagnostic contains: ThreadPriorityCheck",
            "    Thread.yield();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setPriority() {
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
            "    // BUG: Diagnostic contains: ThreadPriorityCheck",
            "    thread.setPriority(Thread.MAX_PRIORITY);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
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
