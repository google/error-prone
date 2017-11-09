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

package com.google.errorprone.matchers;

import static org.junit.Assert.assertTrue;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author deminguyen@google.com (Demi Nguyen) */
@RunWith(JUnit4.class)
public class MethodReturnsNonNullStringTest extends CompilerBasedAbstractTest {

  @Test
  public void shouldMatchInstanceMethod() {
    writeFile(
        "A.java",
        "public class A {",
        "  public void testInstanceMethods() {",
        "    String testStr = \"test string\";",
        "    testStr.charAt(0);",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ true, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldMatchStaticMethod() {
    writeFile(
        "A.java",
        "public class A {",
        "  public void testStaticMethods() {",
        "    String.valueOf(123);",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ true, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldNotMatchConstructorInvocation() {
    writeFile(
        "A.java",
        "public class A {",
        "  public String getString() {",
        "    String str = new String(\"hi\");",
        "    return str;",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ false, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldNotMatchOtherClasses() {
    writeFile(
        "A.java",
        "public class A {",
        "  public String getString() {",
        "    return \"test string\";",
        "  }",
        "  public void testMethodInvocation() {",
        "    getString();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ false, Matchers.methodReturnsNonNull()));
  }

  private Scanner methodInvocationMatches(
      final boolean shouldMatch, final Matcher<ExpressionTree> toMatch) {
    return new Scanner() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
        ExpressionTree methodSelect = node.getMethodSelect();
        if (!methodSelect.toString().equals("super")) {
          assertTrue(methodSelect.toString(), !shouldMatch ^ toMatch.matches(node, visitorState));
        }
        return super.visitMethodInvocation(node, visitorState);
      }
    };
  }
}
