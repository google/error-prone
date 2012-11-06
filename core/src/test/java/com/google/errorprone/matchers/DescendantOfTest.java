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

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class DescendantOfTest extends CompilerBasedTest {

  @Before
  public void setUp() throws IOException {
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

  private Scanner memberSelectMatches(final boolean shouldMatch, final DescendantOf toMatch) {
    return new Scanner() {
      @Override
      public Void visitMemberSelect(MemberSelectTree node, VisitorState visitorState) {
        if (getCurrentPath().getParentPath().getLeaf().getKind() == Kind.METHOD_INVOCATION) {
          assertTrue(node.toString(),
              !shouldMatch ^ toMatch.matches(node, visitorState));
        }
        return super.visitMemberSelect(node, visitorState);
      }
    };
  }

}
