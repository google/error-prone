/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class StaticMethodTest extends CompilerBasedAbstractTest {

  @Before
  public void setUp() {
    writeFile("A.java",
        "package com.google;",
        "public class A { ",
        "  public static int count() {",
        "    return 1;",
        "  }",
        "  public int instanceCount() {",
        "    return 2;",
        "  }",
        "  public static int withArgument(String s) {",
        "    return 3;",
        "  }",
        "}"
    );
  }

  @Test
  public void shouldMatchUsingImportStatements() {
    writeFile("B.java",
      "import com.google.A;",
      "public class B {",
      "  public int count() {",
      "    return A.count();",
      "  }",
      "}"
    );
    assertCompiles(methodInvocationMatches(true, new StaticMethod("com.google.A", "count")));
    assertCompiles(methodInvocationMatches(true, new StaticMethod("*", "count")));
    assertCompiles(methodInvocationMatches(true, new StaticMethod("com.google.A", "*")));
    assertCompiles(methodInvocationMatches(true, new StaticMethod("*", "*")));
    assertCompiles(methodInvocationMatches(true, new StaticMethod("com.google.A", "count()")));
  }

  @Test
  public void shouldOnlyMatchFullSignature() {
    writeFile("B.java",
      "import com.google.A;",
      "public class B {",
      "  public int count() {",
      "    return A.withArgument(\"foo\");",
      "  }",
      "}"
    );
    assertCompiles(methodInvocationMatches(true,
          new StaticMethod("com.google.A", "withArgument(java.lang.String)")));
    assertCompiles(methodInvocationMatches(false,
          new StaticMethod("com.google.A", "withArgument()")));
    assertCompiles(methodInvocationMatches(false,
          new StaticMethod("com.google.A", "withArgument(String)")));
  }

  @Test
  public void shouldMatchFullyQualifiedCallSite() {
    writeFile("B.java",
      "public class B {",
      "  public int count() {",
      "    return com.google.A.count();",
      "  }",
      "}"
    );
    assertCompiles(methodInvocationMatches(true, new StaticMethod("com.google.A", "count")));
  }

  @Test
  public void shouldNotMatchWhenPackageDiffers() {
    writeFile("B.java",
        "public class B {",
        "  static class A {",
        "    public static int count() { return 0; }",
        "  }",
        "  public int count() {",
        "    return A.count();",
        "  }",
        "}"
    );
    assertCompiles(methodInvocationMatches(false, new StaticMethod("com.google.A", "count")));
    assertCompiles(methodInvocationMatches(false, new StaticMethod("com.google.A", "*")));
  }

  @Test
  public void shouldNotMatchInstanceMethod() {
    writeFile("B.java",
        "import com.google.A;",
        "public class B {",
        "  public int count() {",
        "    A a = new A();",
        "    return a.instanceCount();",
        "  }",
        "}"
    );
    assertCompiles(methodInvocationMatches(false,
        new StaticMethod("com.google.A", "instanceCount")));
    assertCompiles(methodInvocationMatches(false, new StaticMethod("*", "*")));
  }

  @Test
  public void shouldMatchStaticImport() {
    writeFile("B.java",
        "import static com.google.A.count;",
        "public class B {",
        "  public int bCount() {",
        "    return count();",
        "  }",
        "}"
    );
    assertCompiles(methodInvocationMatches(true, new StaticMethod("com.google.A", "count")));
  }

  private Scanner methodInvocationMatches(final boolean shouldMatch, final StaticMethod toMatch) {
    return new Scanner() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
        ExpressionTree methodSelect = node.getMethodSelect();
        if (!methodSelect.toString().equals("super")) {
          assertTrue(methodSelect.toString(),
                !shouldMatch ^ toMatch.matches(methodSelect, visitorState));
        }
        return super.visitMethodInvocation(node, visitorState);
      }
    };
  }

}
