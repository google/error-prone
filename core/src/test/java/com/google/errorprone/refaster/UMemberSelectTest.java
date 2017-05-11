/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import static org.mockito.Mockito.mock;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UMemberSelect}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UMemberSelectTest extends AbstractUTreeTest {
  @Test
  public void inline() {
    ULiteral fooLit = ULiteral.stringLit("foo");
    UType type = mock(UType.class);
    UMemberSelect memberSelect = UMemberSelect.create(fooLit, "length", type);
    assertInlines("\"foo\".length", memberSelect);
  }

  @Test
  public void equality() {
    UType stringTy = UClassType.create("java.lang.String");

    // int String.indexOf(int)
    UMethodType indexOfIntTy = UMethodType.create(UPrimitiveType.INT, UPrimitiveType.INT);
    // int String.indexOf(String)
    UMethodType indexOfStringTy = UMethodType.create(UPrimitiveType.INT, stringTy);

    UExpression fooLit = ULiteral.stringLit("foo");
    UExpression barLit = ULiteral.stringLit("bar");

    new EqualsTester()
        .addEqualityGroup(UMemberSelect.create(fooLit, "indexOf", indexOfIntTy))
        .addEqualityGroup(UMemberSelect.create(fooLit, "indexOf", indexOfStringTy))
        .addEqualityGroup(UMemberSelect.create(barLit, "indexOf", indexOfIntTy))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UMemberSelect.create(
            ULiteral.stringLit("foo"),
            "indexOf",
            UMethodType.create(UPrimitiveType.INT, UPrimitiveType.INT)));
  }
}
