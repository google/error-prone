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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UMethodInvocation}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UMethodInvocationTest extends AbstractUTreeTest {
  @Test
  public void match() {
    UExpression fooIdent = mock(UExpression.class);
    when(fooIdent.unify(ident("foo"), isA(Unifier.class))).thenReturn(Choice.of(unifier));
    ULiteral oneLit = ULiteral.intLit(1);
    ULiteral barLit = ULiteral.stringLit("bar");
    UMethodInvocation invocation =
        UMethodInvocation.create(fooIdent, ImmutableList.<UExpression>of(oneLit, barLit));
    assertUnifies("foo(1, \"bar\")", invocation);
  }

  @Test
  public void inline() throws CouldNotResolveImportException {
    UExpression fooIdent = mock(UExpression.class);
    when(fooIdent.inline(isA(Inliner.class)))
        .thenReturn(inliner.maker().Ident(inliner.asName("foo")));
    ULiteral oneLit = ULiteral.intLit(1);
    ULiteral barLit = ULiteral.stringLit("bar");
    UMethodInvocation invocation = UMethodInvocation.create(fooIdent, oneLit, barLit);
    assertInlines("foo(1, \"bar\")", invocation);
  }

  @Test
  public void equality() {
    UMethodType indexOfIntTy = UMethodType.create(UPrimitiveType.INT, UPrimitiveType.INT);
    UExpression fooLit = ULiteral.stringLit("foo");
    UType isEmptyTy = UMethodType.create(UPrimitiveType.BOOLEAN, ImmutableList.<UType>of());

    new EqualsTester()
        .addEqualityGroup(
            UMethodInvocation.create(
                UMemberSelect.create(fooLit, "indexOf", indexOfIntTy), ULiteral.charLit('a')))
        .addEqualityGroup(
            UMethodInvocation.create(
                UMemberSelect.create(fooLit, "indexOf", indexOfIntTy), ULiteral.charLit('b')))
        .addEqualityGroup(
            UMethodInvocation.create(UMemberSelect.create(fooLit, "isEmpty", isEmptyTy)))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UMethodInvocation.create(
            UMemberSelect.create(
                ULiteral.stringLit("foo"),
                "indexOf",
                UMethodType.create(UPrimitiveType.INT, UPrimitiveType.INT)),
            ULiteral.charLit('a')));
  }
}
