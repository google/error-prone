/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

/** {@link GuardedByValidator}Test */
@RunWith(JUnit4.class)
public class GuardedByValidatorTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = new CompilationTestHelper(GuardedByValidator.class);
  }

  @Test
  public void testPositive() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        CompilationTestHelper.forSourceLines(
            "threadsafety.Test",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  @GuardedBy(\"This thread\") int x;",
            "  // BUG: Diagnostic contains:",
            "  @GuardedBy(\"This thread\") void m() {}",
            "}"
        )
    );
  }

  @Test
  public void testNegative() throws Exception {
    compilationHelper.assertCompileSucceeds(
        CompilationTestHelper.forSourceLines(
            "threadsafety.Test",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  final Object mu = new Object();",
            "  class Inner {",
            "    final Object lock = new Object();",
            "    @GuardedBy(\"this\") int x;",
            "    @GuardedBy(\"Inner.this\") int y;",
            "    @GuardedBy(\"Test.this\") int p;",
            "    @GuardedBy(\"Inner.this.lock\") int z;",
            "    @GuardedBy(\"this\") void m() {}",
            "    @GuardedBy(\"mu\") int v;",
            "    @GuardedBy(\"itself\") Object s_;",
            "  }",
            "  final Object o = new Object() {",
            "    @GuardedBy(\"mu\") int x;",
            "  };",
            "}"
        )
    );
  }

  @Test
  public void testItself() throws Exception {
    compilationHelper.assertCompileSucceeds(
        CompilationTestHelper.forSourceLines(
            "threadsafety.Test",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"itself\") Object s_;",
            "}"
        )
    );
  }

  @Test
  public void testBadInstanceAccess() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        CompilationTestHelper.forSourceLines(
            "threadsafety.Test",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  final Object instanceField = new Object();",
            "  // BUG: Diagnostic contains:",
            "  @GuardedBy(\"Test.instanceField\") Object s_;",
            "}"
        )
    );
  }
}
