/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class DescendantOfTest extends DescendantOfAbstractTest {

  @Test
  public void shouldMatchExactMethod() {
    writeFile(
        "B.java",
        "import com.google.A;",
        "public class B {",
        "  public int count() {",
        "    A a = new A();",
        "    return a.count();",
        "  }",
        "}");
    assertCompiles(
        memberSelectMatches(/* shouldMatch= */ true, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldMatchOverriddenMethod() {
    writeFile(
        "B.java",
        "import com.google.A;",
        "public class B extends A {",
        "  public int count() {",
        "    B b = new B();",
        "    return b.count();",
        "  }",
        "}");
    assertCompiles(
        memberSelectMatches(/* shouldMatch= */ true, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldMatchBareOverriddenMethod() {
    writeFile(
        "B.java",
        "import com.google.A;",
        "public class B extends A {",
        "  public int count() {",
        "    return 2;",
        "  }",
        "  public int testCount() {",
        "    return count();",
        "  }",
        "}");
    assertCompiles(
        memberSelectMatches(/* shouldMatch= */ true, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldNotMatchDifferentMethod() {
    writeFile(
        "B.java",
        "import com.google.A;",
        "public class B {",
        "  public int count() {",
        "    A a = new A();",
        "    return a.count();",
        "  }",
        "}");
    assertCompiles(
        memberSelectMatches(
            /* shouldMatch= */ false, new DescendantOf("com.google.A", "count(java.lang.Object)")));
  }

  @Test
  public void shouldNotMatchStaticMethod() {
    writeFile(
        "B.java",
        "import com.google.A;",
        "public class B {",
        "  public int count() {",
        "    return A.staticCount();",
        "  }",
        "}");
    assertCompiles(
        memberSelectMatches(/* shouldMatch= */ false, new DescendantOf("com.google.A", "count()")));
  }
}
