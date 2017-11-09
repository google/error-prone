/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author adgar@google.com (Mike Edgar) */
@RunWith(JUnit4.class)
public class CompoundAssignmentTest extends CompilerBasedAbstractTest {

  @Test(expected = IllegalArgumentException.class)
  public void cannotConstructWithInvalidKind() {
    Set<Kind> operators = new HashSet<Kind>();
    operators.add(Kind.PLUS_ASSIGNMENT);
    operators.add(Kind.IF);
    assertCompiles(
        compoundAssignmentMatches(
            /* shouldMatch= */ true,
            new CompoundAssignment(
                operators,
                Matchers.<ExpressionTree>anything(),
                Matchers.<ExpressionTree>anything())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannotConstructWithBinaryOperator() {
    Set<Kind> operators = new HashSet<Kind>();
    operators.add(Kind.PLUS);
    operators.add(Kind.PLUS_ASSIGNMENT);
    assertCompiles(
        compoundAssignmentMatches(
            /* shouldMatch= */ true,
            new CompoundAssignment(
                operators,
                Matchers.<ExpressionTree>anything(),
                Matchers.<ExpressionTree>anything())));
  }

  @Test
  public void shouldMatch() {
    writeFile(
        "A.java",
        "public class A {",
        "  public void getHash(int a, long b) {",
        "    long c = a;",
        "    c += b;",
        "  }",
        "}");
    Set<Kind> operators = new HashSet<Kind>();
    operators.add(Kind.PLUS_ASSIGNMENT);
    operators.add(Kind.LEFT_SHIFT_ASSIGNMENT);
    assertCompiles(
        compoundAssignmentMatches(
            /* shouldMatch= */ true,
            new CompoundAssignment(
                operators,
                Matchers.<ExpressionTree>anything(),
                Matchers.<ExpressionTree>anything())));
  }

  @Test
  public void shouldNotMatchWhenOperatorDiffers() {
    writeFile(
        "A.java",
        "public class A {",
        "  public void getHash(int a, long b) {",
        "    long c = a;",
        "    c -= b;",
        "  }",
        "}");
    Set<Kind> operators = new HashSet<Kind>();
    operators.add(Kind.PLUS_ASSIGNMENT);
    assertCompiles(
        compoundAssignmentMatches(
            /* shouldMatch= */ false,
            new CompoundAssignment(
                operators,
                Matchers.<ExpressionTree>anything(),
                Matchers.<ExpressionTree>anything())));
  }

  @Test
  public void shouldNotMatchWhenLeftOperandMatcherFails() {
    writeFile(
        "A.java",
        "public class A {",
        "  public void getHash(int a, long b) {",
        "    long c = a;",
        "    c += b;",
        "  }",
        "}");
    Set<Kind> operators = new HashSet<Kind>();
    operators.add(Kind.PLUS_ASSIGNMENT);
    assertCompiles(
        compoundAssignmentMatches(
            /* shouldMatch= */ false,
            new CompoundAssignment(
                operators,
                Matchers.<ExpressionTree>isArrayType(),
                Matchers.<ExpressionTree>anything())));
  }

  @Test
  public void shouldNotMatchWhenRightOperandMatcherFails() {
    writeFile(
        "A.java",
        "public class A {",
        "  public void getHash(int a, long b) {",
        "    long c = a;",
        "    c += b;",
        "  }",
        "}");
    Set<Kind> operators = new HashSet<Kind>();
    operators.add(Kind.PLUS_ASSIGNMENT);
    assertCompiles(
        compoundAssignmentMatches(
            /* shouldMatch= */ false,
            new CompoundAssignment(
                operators,
                Matchers.<ExpressionTree>anything(),
                Matchers.<ExpressionTree>isArrayType())));
  }

  private Scanner compoundAssignmentMatches(
      final boolean shouldMatch, final CompoundAssignment toMatch) {
    return new Scanner() {
      @Override
      public Void visitCompoundAssignment(CompoundAssignmentTree node, VisitorState visitorState) {
        assertTrue(node.toString(), !shouldMatch ^ toMatch.matches(node, visitorState));
        return super.visitCompoundAssignment(node, visitorState);
      }
    };
  }
}
