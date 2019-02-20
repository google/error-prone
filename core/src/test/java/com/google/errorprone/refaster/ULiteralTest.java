/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.source.tree.Tree.Kind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ULiteral}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class ULiteralTest extends AbstractUTreeTest {
  @Test
  public void nullLiteral() {
    ULiteral lit = ULiteral.nullLit();
    assertUnifiesAndInlines("null", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.NULL_LITERAL);
  }

  @Test
  public void stringLiteral() {
    ULiteral lit = ULiteral.stringLit("foo");
    assertUnifiesAndInlines("\"foo\"", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.STRING_LITERAL);
  }

  @Test
  public void intLiteral() {
    ULiteral lit = ULiteral.intLit(123);
    assertUnifiesAndInlines("123", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.INT_LITERAL);
  }

  @Test
  public void longLiteral() {
    ULiteral lit = ULiteral.longLit(123L);
    assertUnifiesAndInlines("123L", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.LONG_LITERAL);
  }

  @Test
  public void charLiteral() {
    ULiteral lit = ULiteral.create(Kind.CHAR_LITERAL, '%');
    assertUnifiesAndInlines("'%'", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.CHAR_LITERAL);
  }

  @Test
  public void doubleLiteral() {
    ULiteral lit = ULiteral.doubleLit(1.23);
    assertUnifiesAndInlines("1.23", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.DOUBLE_LITERAL);
  }

  @Test
  public void floatLiteral() {
    ULiteral lit = ULiteral.floatLit(1.23F);
    assertUnifiesAndInlines("1.23F", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.FLOAT_LITERAL);
  }

  @Test
  public void booleanLiteral() {
    ULiteral lit = ULiteral.booleanLit(false);
    assertUnifiesAndInlines("false", lit);
    assertThat(lit.getKind()).isEqualTo(Kind.BOOLEAN_LITERAL);
  }

  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(ULiteral.intLit(1))
        .addEqualityGroup(ULiteral.intLit(2))
        .addEqualityGroup(ULiteral.longLit(1L))
        .addEqualityGroup(ULiteral.doubleLit(1.0))
        .addEqualityGroup(ULiteral.stringLit("foo"))
        .addEqualityGroup(ULiteral.nullLit())
        .addEqualityGroup(ULiteral.charLit('0'))
        .addEqualityGroup(ULiteral.intLit('0'))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(ULiteral.intLit(1));
    SerializableTester.reserializeAndAssert(ULiteral.longLit(1L));
    SerializableTester.reserializeAndAssert(ULiteral.doubleLit(1.0));
    SerializableTester.reserializeAndAssert(ULiteral.floatLit(1.0f));
    SerializableTester.reserializeAndAssert(ULiteral.stringLit("foo"));
    SerializableTester.reserializeAndAssert(ULiteral.nullLit());
    SerializableTester.reserializeAndAssert(ULiteral.booleanLit(true));
    SerializableTester.reserializeAndAssert(ULiteral.charLit('0'));
  }
}
