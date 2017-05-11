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
 * Tests for {@link UDoWhileLoop}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UDoWhileLoopTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UDoWhileLoop.create(
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(
                            ULocalVarIdent.create("old"),
                            UMethodInvocation.create(
                                UMemberSelect.create(
                                    UFreeIdent.create("str"),
                                    "indexOf",
                                    UMethodType.create(
                                        UPrimitiveType.INT,
                                        UPrimitiveType.INT,
                                        UPrimitiveType.INT)),
                                ULiteral.charLit(' '),
                                UBinary.create(
                                    Kind.PLUS,
                                    ULocalVarIdent.create("old"),
                                    ULiteral.intLit(1)))))),
                UParens.create(
                    UBinary.create(
                        Kind.NOT_EQUAL_TO, ULocalVarIdent.create("old"), ULiteral.intLit(-1)))))
        .addEqualityGroup(
            UDoWhileLoop.create(
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(
                            ULocalVarIdent.create("old"),
                            UMethodInvocation.create(
                                UMemberSelect.create(
                                    UFreeIdent.create("str"),
                                    "indexOf",
                                    UMethodType.create(
                                        UPrimitiveType.INT,
                                        UPrimitiveType.INT,
                                        UPrimitiveType.INT)),
                                ULiteral.charLit(' '),
                                UBinary.create(
                                    Kind.PLUS,
                                    ULocalVarIdent.create("old"),
                                    ULiteral.intLit(1)))))),
                UParens.create(
                    UBinary.create(
                        Kind.GREATER_THAN_EQUAL,
                        ULocalVarIdent.create("old"),
                        ULiteral.intLit(0)))))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UDoWhileLoop.create(
            UBlock.create(
                UExpressionStatement.create(
                    UAssign.create(
                        ULocalVarIdent.create("old"),
                        UMethodInvocation.create(
                            UMemberSelect.create(
                                UFreeIdent.create("str"),
                                "indexOf",
                                UMethodType.create(
                                    UPrimitiveType.INT, UPrimitiveType.INT, UPrimitiveType.INT)),
                            ULiteral.charLit(' '),
                            UBinary.create(
                                Kind.PLUS, ULocalVarIdent.create("old"), ULiteral.intLit(1)))))),
            UParens.create(
                UBinary.create(
                    Kind.NOT_EQUAL_TO, ULocalVarIdent.create("old"), ULiteral.intLit(-1)))));
  }
}
