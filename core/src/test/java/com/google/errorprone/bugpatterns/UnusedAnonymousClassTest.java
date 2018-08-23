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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnusedAnonymousClass}Test */
@RunWith(JUnit4.class)
public class UnusedAnonymousClassTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(UnusedAnonymousClass.class, getClass());
  }

  @Test
  public void deadObject() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "public class One {",
            "  public static void main(String[] args) {",
            "    new Object();",
            "  }",
            "}")
        .doTest();
  }

  // Thread has a known side-effect free constructor
  @Test
  public void deadThread() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "public class One {",
            "  public static void main(String[] args) {",
            "    // BUG: Diagnostic contains:",
            "    new Thread() {",
            "      public void run() {}",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void liveObject() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "public class One {",
            "  public static void main(String[] args) {",
            "    new Object().toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void deadCallable() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "import java.util.concurrent.Callable;",
            "public class One {",
            "  public static void main(String[] args) throws Exception {",
            "    // BUG: Diagnostic contains:",
            "    new Callable<Void>() {",
            "      public Void call() throws Exception {",
            "        return null;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void liveCallable() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "import java.util.concurrent.Callable;",
            "public class One {",
            "  public static void main(String[] args) throws Exception {",
            "    new Callable<Void>() {",
            "      public Void call() throws Exception {",
            "        return null;",
            "      }",
            "    }.call();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void liveCallableViaCinit() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "import java.util.concurrent.Callable;",
            "import java.util.ArrayList;",
            "public class One {",
            "  static ArrayList<Callable<Void>> callables = new ArrayList<>();",
            "  public static void main(String[] args) throws Exception {",
            "    new Callable<Void>() {",
            "      { callables.add(this); }",
            "      public Void call() throws Exception {",
            "        return null;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void deadCallableWithField() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "import java.util.concurrent.Callable;",
            "import java.util.ArrayList;",
            "public class One {",
            "  public static void main(String[] args) throws Exception {",
            "    // BUG: Diagnostic contains:",
            "    new Callable<Void>() {",
            "      Void register;",
            "      public Void call() throws Exception {",
            "        return null;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void liveCallableViaField() {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "import java.util.concurrent.Callable;",
            "import java.util.ArrayList;",
            "public class One {",
            "  static ArrayList<Callable<Void>> callables = new ArrayList<>();",
            "  static Void register(Callable<Void> callable) {",
            "    callables.add(callable);",
            "    return null;",
            "  }",
            "  public static void main(String[] args) throws Exception {",
            "    new Callable<Void>() {",
            "      Void register = register(this);",
            "      public Void call() throws Exception {",
            "        return null;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
