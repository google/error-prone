/*
 * Copyright 2019 The Error Prone Authors.
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

/** {@link Incomparable}Test */
@RunWith(JUnit4.class)
public class IncomparableTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Incomparable.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.TreeMap;",
            "import java.util.TreeSet;",
            "import java.util.Set;",
            "import java.util.Collections;",
            "final class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: Test",
            "    new TreeMap<Test, String>();",
            "    // BUG: Diagnostic contains: Test",
            "    new TreeSet<Test>();",
            "    // BUG: Diagnostic contains: Test",
            "    Set<Test> xs = Collections.synchronizedSet(new TreeSet<>());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.TreeMap;",
            "import java.util.TreeSet;",
            "import java.util.Set;",
            "import java.util.Collections;",
            "class Test {",
            "  void f() {",
            "    new TreeMap<String, Test>();",
            "    new TreeSet<String>();",
            "    Set<String> xs = Collections.synchronizedSet(new TreeSet<>());",
            "  }",
            "}")
        .doTest();
  }
}
