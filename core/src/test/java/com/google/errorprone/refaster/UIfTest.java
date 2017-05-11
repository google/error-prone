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

import com.google.common.base.Joiner;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UIf}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UIfTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UIf.create(
                UFreeIdent.create("cond"),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("z"))))))
        .addEqualityGroup(
            UIf.create(
                UFreeIdent.create("cond"),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
                null))
        .addEqualityGroup(
            UIf.create(
                ULiteral.booleanLit(true),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("z"))))))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UIf.create(
            UFreeIdent.create("cond"),
            UBlock.create(
                UExpressionStatement.create(
                    UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
            UBlock.create(
                UExpressionStatement.create(
                    UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("z"))))));
  }

  @Test
  public void inlineWithElse() {
    UIf ifTree =
        UIf.create(
            UFreeIdent.create("cond"),
            UBlock.create(
                UExpressionStatement.create(
                    UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
            UBlock.create(
                UExpressionStatement.create(
                    UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("z")))));
    bind(new UFreeIdent.Key("cond"), parseExpression("true"));
    bind(new UFreeIdent.Key("x"), parseExpression("x"));
    bind(new UFreeIdent.Key("y"), parseExpression("\"foo\""));
    bind(new UFreeIdent.Key("z"), parseExpression("\"bar\""));
    assertInlines(
        Joiner.on(System.lineSeparator())
            .join(
                "if (true) {", //
                "    x = \"foo\";",
                "} else {",
                "    x = \"bar\";",
                "}"),
        ifTree);
  }

  @Test
  public void inlineWithoutElse() {
    UIf ifTree =
        UIf.create(
            UFreeIdent.create("cond"),
            UBlock.create(
                UExpressionStatement.create(
                    UAssign.create(UFreeIdent.create("x"), UFreeIdent.create("y")))),
            null);
    bind(new UFreeIdent.Key("cond"), parseExpression("true"));
    bind(new UFreeIdent.Key("x"), parseExpression("x"));
    bind(new UFreeIdent.Key("y"), parseExpression("\"foo\""));
    assertInlines(
        Joiner.on(System.lineSeparator())
            .join(
                "if (true) {", //
                "    x = \"foo\";",
                "}"),
        ifTree);
  }
}
