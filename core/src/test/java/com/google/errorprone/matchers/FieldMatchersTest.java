/*
 * Copyright 2026 The Error Prone Authors.
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
import com.sun.source.tree.MemberSelectTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldMatchersTest extends CompilerBasedAbstractTest {

  @Test
  public void compilesWithPrimitiveClassLiteral() {
    writeFile(
        "A.java",
        """
        class A {
          Class<?> clazz = long.class;
        }
        """);

    assertCompiles(
        memberSelectMatches(
            /* shouldMatch= */ false, FieldMatchers.anyFieldInClass("A")));
    assertCompiles(
            memberSelectMatches(
                    /* shouldMatch= */ false, FieldMatchers.instanceField("A", "irrelevant")));
    assertCompiles(
            memberSelectMatches(
                    /* shouldMatch= */ false, FieldMatchers.staticField("A", "irrelevant")));
  }

  private static Scanner memberSelectMatches(
      boolean shouldMatch, Matcher<ExpressionTree> matcher) {
    return new Scanner() {
      @Override
      public Void visitMemberSelect(MemberSelectTree node, VisitorState visitorState) {
        assertWithMessage(node.toString())
            .that(matcher.matches(node, visitorState))
            .isEqualTo(shouldMatch);
        return super.visitMemberSelect(node, visitorState);
      }
    };
  }
}
