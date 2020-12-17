/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author deminguyen@google.com (Demi Nguyen) */
@RunWith(JUnit4.class)
public class MethodReturnsNonNullToStringTest extends CompilerBasedAbstractTest {

  @Test
  public void shouldMatch() {
    writeFile(
        "A.java",
        "public class A {",
        "  public String testToString() {",
        "    Object obj = new Object();",
        "    return obj.toString();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ true, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldMatchDescendants() {
    writeFile(
        "A.java",
        "public class A {",
        "  public String testThisToString() {",
        "    return toString();",
        "  }",
        "  public String testInstanceToString() {",
        "    Object o = new Object();",
        "    return o.toString();",
        "  }",
        "  public String testStringToString() {",
        "    String str = \"a string\";",
        "    return str.toString();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ true, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldMatchBareOverride() {
    writeFile(
        "A.java",
        "public class A {",
        "  public String toString() {",
        "    return \"a string\";",
        "  }",
        "  public String testToString() {",
        "    return toString();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ true, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shouldNotMatchWhenMethodNameDiffers() {
    writeFile(
        "A.java",
        "public class A {",
        "  public String ToString() {",
        "    return \"match should be case sensitive\";",
        "  }",
        "  public String testMethodWithDifferentCase() {",
        "    return ToString();",
        "  }",
        "}");
    assertCompiles(
        methodInvocationMatches(/* shouldMatch= */ false, Matchers.methodReturnsNonNull()));
  }

  @Test
  public void shoudlNotMatchWhenMethodSignatureDiffers() {
    writeFile(
        "A.java",
        "  public String toString(int i) {",
        "    return \"toString method with param\";",
        "  }",
        "  public String testMethodWithParam() {",
        "    return toString(3);",
        "  }",
        "}");
  }

  private Scanner methodInvocationMatches(
      final boolean shouldMatch, final Matcher<ExpressionTree> toMatch) {
    return new Scanner() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
        ExpressionTree methodSelect = node.getMethodSelect();
        if (!methodSelect.toString().equals("super")) {
          assertWithMessage(methodSelect.toString())
              .that(!shouldMatch ^ toMatch.matches(node, visitorState))
              .isTrue();
        }
        return super.visitMethodInvocation(node, visitorState);
      }
    };
  }
}
