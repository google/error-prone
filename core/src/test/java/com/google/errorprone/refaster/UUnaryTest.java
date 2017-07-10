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

import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.source.tree.Tree.Kind;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UUnary}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UUnaryTest extends AbstractUTreeTest {
  UExpression fooIdent;

  @Before
  public void initializeFooIdentifier() throws CouldNotResolveImportException {
    fooIdent = mock(UExpression.class);
    when(fooIdent.unify(ident("foo"), isA(Unifier.class))).thenReturn(Choice.of(unifier));
    when(fooIdent.inline(isA(Inliner.class)))
        .thenReturn(inliner.maker().Ident(inliner.asName("foo")));
  }

  @Test
  public void rejectsNonUnaryOperations() {
    ULiteral sevenLit = ULiteral.intLit(7);
    assertThrows(IllegalArgumentException.class, () -> UUnary.create(Kind.PLUS, sevenLit));
  }

  @Test
  public void complement() {
    assertUnifiesAndInlines("~7", UUnary.create(Kind.BITWISE_COMPLEMENT, ULiteral.intLit(7)));
  }

  @Test
  public void logicalNegation() {
    assertUnifiesAndInlines(
        "!false", UUnary.create(Kind.LOGICAL_COMPLEMENT, ULiteral.booleanLit(false)));
  }

  @Test
  public void unaryPlus() {
    assertUnifiesAndInlines("+foo", UUnary.create(Kind.UNARY_PLUS, fooIdent));
  }

  @Test
  public void unaryNegation() {
    assertUnifiesAndInlines("-foo", UUnary.create(Kind.UNARY_MINUS, fooIdent));
  }

  @Test
  public void preIncrement() {
    assertUnifiesAndInlines("++foo", UUnary.create(Kind.PREFIX_INCREMENT, fooIdent));
  }

  @Test
  public void postIncrement() {
    assertUnifiesAndInlines("foo++", UUnary.create(Kind.POSTFIX_INCREMENT, fooIdent));
  }

  @Test
  public void preDecrement() {
    assertUnifiesAndInlines("--foo", UUnary.create(Kind.PREFIX_DECREMENT, fooIdent));
  }

  @Test
  public void postDecrement() {
    assertUnifiesAndInlines("foo--", UUnary.create(Kind.POSTFIX_DECREMENT, fooIdent));
  }

  @Test
  public void equality() {
    ULiteral sevenLit = ULiteral.intLit(7);
    ULiteral threeLit = ULiteral.intLit(3);
    ULiteral falseLit = ULiteral.booleanLit(false);
    new EqualsTester()
        .addEqualityGroup(UUnary.create(Kind.UNARY_MINUS, sevenLit))
        .addEqualityGroup(UUnary.create(Kind.UNARY_MINUS, threeLit))
        .addEqualityGroup(UUnary.create(Kind.BITWISE_COMPLEMENT, sevenLit))
        .addEqualityGroup(UUnary.create(Kind.LOGICAL_COMPLEMENT, falseLit))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UUnary.create(Kind.BITWISE_COMPLEMENT, ULiteral.intLit(7)));
  }
}
