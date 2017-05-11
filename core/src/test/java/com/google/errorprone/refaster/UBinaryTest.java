/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.source.tree.Tree.Kind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UBinary}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UBinaryTest extends AbstractUTreeTest {
  @Test
  public void plus() {
    assertUnifiesAndInlines(
        "4 + 17", UBinary.create(Kind.PLUS, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void minus() {
    assertUnifiesAndInlines(
        "4 - 17", UBinary.create(Kind.MINUS, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void times() {
    assertUnifiesAndInlines(
        "4 * 17", UBinary.create(Kind.MULTIPLY, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void divide() {
    assertUnifiesAndInlines(
        "4 / 17", UBinary.create(Kind.DIVIDE, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void mod() {
    assertUnifiesAndInlines(
        "4 % 17", UBinary.create(Kind.REMAINDER, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void lessThan() {
    assertUnifiesAndInlines(
        "4 < 17", UBinary.create(Kind.LESS_THAN, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void lessThanOrEqual() {
    assertUnifiesAndInlines(
        "4 <= 17", UBinary.create(Kind.LESS_THAN_EQUAL, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void greaterThan() {
    assertUnifiesAndInlines(
        "4 > 17", UBinary.create(Kind.GREATER_THAN, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void greaterThanOrEqual() {
    assertUnifiesAndInlines(
        "4 >= 17",
        UBinary.create(Kind.GREATER_THAN_EQUAL, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void equal() {
    assertUnifiesAndInlines(
        "4 == 17", UBinary.create(Kind.EQUAL_TO, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void notEqual() {
    assertUnifiesAndInlines(
        "4 != 17", UBinary.create(Kind.NOT_EQUAL_TO, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void leftShift() {
    assertUnifiesAndInlines(
        "4 << 17", UBinary.create(Kind.LEFT_SHIFT, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void signedRightShift() {
    assertUnifiesAndInlines(
        "4 >> 17", UBinary.create(Kind.RIGHT_SHIFT, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void unsignedRightShift() {
    assertUnifiesAndInlines(
        "4 >>> 17",
        UBinary.create(Kind.UNSIGNED_RIGHT_SHIFT, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void bitwiseAnd() {
    assertUnifiesAndInlines(
        "4 & 17", UBinary.create(Kind.AND, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void bitwiseOr() {
    assertUnifiesAndInlines(
        "4 | 17", UBinary.create(Kind.OR, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void bitwiseXor() {
    assertUnifiesAndInlines(
        "4 ^ 17", UBinary.create(Kind.XOR, ULiteral.intLit(4), ULiteral.intLit(17)));
  }

  @Test
  public void conditionalAnd() {
    assertUnifiesAndInlines(
        "true && false",
        UBinary.create(
            Kind.CONDITIONAL_AND, ULiteral.booleanLit(true), ULiteral.booleanLit(false)));
  }

  @Test
  public void conditionalOr() {
    assertUnifiesAndInlines(
        "true || false",
        UBinary.create(Kind.CONDITIONAL_OR, ULiteral.booleanLit(true), ULiteral.booleanLit(false)));
  }

  @Test
  public void equality() {
    ULiteral oneLit = ULiteral.intLit(1);
    ULiteral twoLit = ULiteral.intLit(2);
    ULiteral piLit = ULiteral.doubleLit(Math.PI);
    ULiteral trueLit = ULiteral.booleanLit(true);
    ULiteral falseLit = ULiteral.booleanLit(false);

    new EqualsTester()
        .addEqualityGroup(UBinary.create(Kind.PLUS, oneLit, twoLit))
        .addEqualityGroup(UBinary.create(Kind.PLUS, oneLit, piLit))
        .addEqualityGroup(UBinary.create(Kind.PLUS, piLit, twoLit))
        .addEqualityGroup(UBinary.create(Kind.MINUS, oneLit, twoLit))
        .addEqualityGroup(UBinary.create(Kind.XOR, oneLit, twoLit))
        .addEqualityGroup(UBinary.create(Kind.CONDITIONAL_OR, trueLit, falseLit))
        .addEqualityGroup(UBinary.create(Kind.OR, trueLit, falseLit))
        .testEquals();
  }

  @Test
  public void serialization() {
    ULiteral oneLit = ULiteral.intLit(1);
    ULiteral twoLit = ULiteral.intLit(2);
    ULiteral piLit = ULiteral.doubleLit(Math.PI);
    ULiteral trueLit = ULiteral.booleanLit(true);
    ULiteral falseLit = ULiteral.booleanLit(false);

    SerializableTester.reserializeAndAssert(UBinary.create(Kind.PLUS, oneLit, twoLit));
    SerializableTester.reserializeAndAssert(UBinary.create(Kind.PLUS, oneLit, piLit));
    SerializableTester.reserializeAndAssert(UBinary.create(Kind.PLUS, piLit, twoLit));
    SerializableTester.reserializeAndAssert(UBinary.create(Kind.MINUS, oneLit, twoLit));
    SerializableTester.reserializeAndAssert(UBinary.create(Kind.XOR, oneLit, twoLit));
    SerializableTester.reserializeAndAssert(UBinary.create(Kind.CONDITIONAL_OR, trueLit, falseLit));
    SerializableTester.reserializeAndAssert(UBinary.create(Kind.OR, trueLit, falseLit));
  }
}
