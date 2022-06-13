/*
 * Copyright 2021 The Error Prone Authors.
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

/** Unit tests for {@link MissingImplementsComparable} check. */
@RunWith(JUnit4.class)
public final class MissingImplementsComparableTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingImplementsComparable.class, getClass());

  @Test
  public void flagsMissingImplementsComparable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  public int compareTo(Test o) {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotFlagImplementsComparable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test implements Comparable<Test> {",
            "  @Override",
            "  public int compareTo(Test o) {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotFlagComparableSuperType() {
    compilationHelper
        .addSourceLines("Foo.java", "abstract class Foo<T> implements Comparable<Foo<T>> {}")
        .addSourceLines(
            "Test.java",
            "class Test extends Foo<String> {",
            "  @Override",
            "  public int compareTo(Foo<String> o) {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotFlagImproperCompareTo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int compareTo(Object o) {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }
}
