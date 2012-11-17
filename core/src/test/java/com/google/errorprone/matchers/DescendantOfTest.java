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

import static org.junit.Assert.assertTrue;

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class DescendantOfTest extends CompilerBasedTest {

  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @Before
  public void setUp() throws IOException {
    tests.clear();
    writeFile("A.java",
        "package com.google;",
        "public class A { ",
        "  public int count() {",
        "    return 1;",
        "  }",
        "  public static int staticCount() {",
        "    return 2;",
        "  }",
        "}"
    );
  }

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void shouldMatchExactMethod() throws IOException {
    writeFile("B.java",
      "import com.google.A;",
      "public class B {",
      "  public int count() {",
      "    A a = new A();",
      "    return a.count();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(true, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldMatchOverriddenMethod() throws IOException {
    writeFile("B.java",
      "import com.google.A;",
      "public class B extends A {",
      "  public int count() {",
      "    B b = new B();",
      "    return b.count();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(true, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldMatchBareOverriddenMethod() throws IOException {
    writeFile("B.java",
      "import com.google.A;",
      "public class B extends A {",
      "  public int count() {",
      "    return 2;",
      "  }",
      "  public int testCount() {",
      "    return count();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(true, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldNotMatchDifferentMethod() throws IOException {
    writeFile("B.java",
      "import com.google.A;",
      "public class B {",
      "  public int count() {",
      "    A a = new A();",
      "    return a.count();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(false,
        new DescendantOf("com.google.A", "count(java.lang.Object)")));
  }

  @Test
  public void shouldNotMatchStaticMethod() throws IOException {
    writeFile("B.java",
      "import com.google.A;",
      "public class B {",
      "  public int count() {",
      "    return A.staticCount();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(false, new DescendantOf("com.google.A", "count()")));
  }

  @Test
  public void shouldMatchTransitively() throws Exception {
    writeFile("I1.java",
      "package i;",
      "public interface I1 {",
      "  void count();",
      "}"
    );
    writeFile("I2.java",
      "package i;",
      "public interface I2 extends I1 {",
      "}"
    );
    writeFile("B.java",
      "package b;",
      "public class B implements i.I2 {",
      "  public void count() {",
      "  }",
      "}"
    );
    assertCompiles(new Scanner());
    clearSourceFiles();
    writeFile("C.java",
        "public class C {",
        "  public void test(b.B b) {",
        "    b.count();",
        "  }",
        "}"
      );
    assertCompiles(memberSelectMatches(true, new DescendantOf("i.I1", "count()")));
  }

  private abstract class ScannerTest extends Scanner {
    public abstract void assertDone();
  }

  private Scanner memberSelectMatches(final boolean shouldMatch, final DescendantOf toMatch) {
    ScannerTest test = new ScannerTest() {
      private boolean matched = false;

      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
        ExpressionTree methodSelect = node.getMethodSelect();
        if (toMatch.matches(methodSelect, visitorState)) {
          matched = true;
        }
        return super.visitMethodInvocation(node, visitorState);
      }

      @Override
      public void assertDone() {
        assertTrue(shouldMatch == matched);
      }
    };
    tests.add(test);
    return test;
  }

}
