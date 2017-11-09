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
public class MethodReturnsNonNullNextTokenTest extends CompilerBasedAbstractTest {

  @Test
  public void shouldMatch() {
    writeFile(
        "A.java",
        "import java.util.StringTokenizer;",
        "public class A {",
        "  public void testNextToken() {",
        "    StringTokenizer st = new StringTokenizer(\"test string\");",
        "    st.nextToken();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ true, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldNotMatchOtherMethod() {
    writeFile(
        "A.java",
        "import java.util.StringTokenizer;",
        "public class A {",
        "  public void testOtherMethod() {",
        "    StringTokenizer st = new StringTokenizer(\"test string\");",
        "    st.hasMoreTokens();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ false, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldNotMatchOverridenMethod() {
    writeFile(
        "A.java",
        "import java.util.StringTokenizer;",
        "public class A extends StringTokenizer {",
        "  public A(String str, String delim, boolean returnDelims) {",
        "    super(str, delim, returnDelims);",
        "  }",
        "  @Override",
        "  public String nextToken() {",
        "    return \"overridden method\";",
        "  }",
        "  public void testOverridenNextToken() {",
        "    nextToken();",
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
