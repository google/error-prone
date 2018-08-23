/*
 * Copyright 2014 The Error Prone Authors.
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

/** {@link GuardedByChecker} annotation syntax GuardedByValidationResult test */
@RunWith(JUnit4.class)
public class GuardedByValidatorTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(GuardedByChecker.class, getClass());
  }

  @Test
  public void testPositive() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: could not resolve guard",
            "  @GuardedBy(\"This thread\") int x;",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: could not resolve guard",
            "  @GuardedBy(\"This thread\") void m() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNegative() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  final Object mu = new Object();",
            "  class Inner {",
            "    @GuardedBy(\"this\") int x;",
            "    @GuardedBy(\"Test.this\") int p;",
            "    @GuardedBy(\"Test.this.mu\") int z;",
            "    @GuardedBy(\"this\") void m() {}",
            "    @GuardedBy(\"mu\") int v;",
            "    @GuardedBy(\"itself\") Object s_;",
            "  }",
            "  final Object o = new Object() {",
            "    @GuardedBy(\"mu\") int x;",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void testItself() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"itself\") Object s_;",
            "}")
        .doTest();
  }

  @Test
  public void testBadInstanceAccess() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  final Object instanceField = new Object();",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: could not resolve guard",
            "  @GuardedBy(\"Test.instanceField\") Object s_;",
            "}")
        .doTest();
  }

  @Test
  public void testClassName() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: could not resolve guard",
            "  @GuardedBy(\"Test\") Object s_;",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClassTypo() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  static class Endpoint {",
            "    Object getLock() { return null; }",
            "  }",
            "  abstract static class Runnable {",
            "    private Endpoint endpoint;",
            "    Runnable(Endpoint endpoint) {",
            "      this.endpoint = endpoint;",
            "    }",
            "    abstract void run();",
            "  }",
            "  static void m(Endpoint endpoint) {",
            "    Runnable runnable =",
            "      new Runnable(endpoint) {",
            "        // BUG: Diagnostic contains:",
            "        // Invalid @GuardedBy expression: could not resolve guard",
            "        @GuardedBy(\"endpoint_.getLock()\") void run() {}",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClassPrivateAccess() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  static class Endpoint {",
            "    Object getLock() { return null; }",
            "  }",
            "  abstract static class Runnable {",
            "    private Endpoint endpoint;",
            "    Runnable(Endpoint endpoint) {",
            "      this.endpoint = endpoint;",
            "    }",
            "    abstract void run();",
            "  }",
            "  static void m(Endpoint endpoint) {",
            "    Runnable runnable =",
            "      new Runnable(endpoint) {",
            "        // BUG: Diagnostic contains:",
            "        // Invalid @GuardedBy expression: could not resolve guard",
            "        @GuardedBy(\"endpoint.getLock()\") void run() {}",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticGuardedByInstance() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: static member guarded by instance",
            "  @GuardedBy(\"this\") static int x;",
            "}")
        .doTest();
  }

  @Test
  public void testStaticGuardedByInstanceMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  final Object mu_ = new Object();",
            "  Object lock() { return mu_; }",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: static member guarded by instance",
            "  @GuardedBy(\"lock()\") static int x;",
            "}")
        .doTest();
  }

  @Test
  public void testStaticGuardedByStatic() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"Test.class\") static int x;",
            "}")
        .doTest();
  }

  @Test
  public void testNonExistantMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: could not resolve guard",
            "  @GuardedBy(\"lock()\") int x;",
            "  // BUG: Diagnostic contains:",
            "  // Invalid @GuardedBy expression: could not resolve guard",
            "  @GuardedBy(\"lock()\") void m() {}",
            "}")
        .doTest();
  }
}
