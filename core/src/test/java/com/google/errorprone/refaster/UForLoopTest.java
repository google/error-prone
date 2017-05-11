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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.source.tree.Tree.Kind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UForLoop}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UForLoopTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UForLoop.create(
                ImmutableList.of(
                    UVariableDecl.create("i", UPrimitiveTypeTree.INT, UFreeIdent.create("from"))),
                UBinary.create(Kind.LESS_THAN, ULocalVarIdent.create("i"), UFreeIdent.create("to")),
                ImmutableList.of(
                    UExpressionStatement.create(
                        UUnary.create(Kind.POSTFIX_INCREMENT, ULocalVarIdent.create("i")))),
                UBlock.create()))
        .addEqualityGroup(
            UForLoop.create(
                ImmutableList.of(
                    UVariableDecl.create("i", UPrimitiveTypeTree.INT, UFreeIdent.create("from"))),
                UBinary.create(Kind.LESS_THAN, ULocalVarIdent.create("i"), UFreeIdent.create("to")),
                ImmutableList.<UExpressionStatement>of(),
                UBlock.create(
                    UExpressionStatement.create(
                        UUnary.create(Kind.POSTFIX_INCREMENT, ULocalVarIdent.create("i"))))))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UForLoop.create(
            ImmutableList.of(
                UVariableDecl.create("i", UPrimitiveTypeTree.INT, UFreeIdent.create("from"))),
            UBinary.create(Kind.LESS_THAN, ULocalVarIdent.create("i"), UFreeIdent.create("to")),
            ImmutableList.of(
                UExpressionStatement.create(
                    UUnary.create(Kind.POSTFIX_INCREMENT, ULocalVarIdent.create("i")))),
            UBlock.create()));
  }
}
