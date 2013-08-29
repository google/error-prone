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

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;

import org.junit.Test;

import java.io.IOException;

/**
 * @author eaftan@google.com (Eddie Aftandilian
 */
public class InstanceMethodTest extends CompilerBasedTest {

  @Test
  public void shouldMatch() throws IOException {
    writeFile("A.java",
      "public class A {",
      "  public int getHash() {",
      "    int[] arr = new int[10];",
      "    return arr.hashCode();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(true, new InstanceMethod(
        Matchers.<ExpressionTree>isArrayType(), "hashCode")));
  }

  @Test
  public void shouldMatchWildCard() throws IOException {
    writeFile("A.java",
        "public class A {",
        "  public int getHash() {",
        "    int[] arr = new int[10];",
        "    return arr.hashCode();",
        "  }",
        "}"
      );
      assertCompiles(memberSelectMatches(true, new InstanceMethod(
          Matchers.<ExpressionTree>isArrayType(), "*")));
  }

  @Test
  public void shouldNotMatchWhenMethodNamesDiffer() throws IOException {
    writeFile("A.java",
      "public class A {",
      "  public int getHash() {",
      "    int[] arr = new int[10];",
      "    return arr.hashCode();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(false, new InstanceMethod(
        Matchers.<ExpressionTree>isArrayType(), "notHashCode")));
  }

  @Test
  public void shouldNotMatchWhenMatcherFails() throws IOException {
    writeFile("A.java",
      "public class A {",
      "  public int getHash() {",
      "    Object obj = new Object();",
      "    return obj.hashCode();",
      "  }",
      "}"
    );
    assertCompiles(memberSelectMatches(false, new InstanceMethod(
        Matchers.<ExpressionTree>isArrayType(), "hashCode")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldNotMatchStaticMethod() throws IOException {
    writeFile("A.java",
        "package com.google;",
        "public class A { ",
        "  public static int count() {",
        "    return 1;",
        "  }",
        "}"
    );
    writeFile("B.java",
        "import com.google.A;",
        "public class B {",
        "  public int count() {",
        "    return A.count();",
        "  }",
        "}"
      );
    assertCompiles(memberSelectMatches(false, new InstanceMethod(
        Matchers.<ExpressionTree>anything(), "count")));
  }

  private Scanner memberSelectMatches(final boolean shouldMatch, final InstanceMethod toMatch) {
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
