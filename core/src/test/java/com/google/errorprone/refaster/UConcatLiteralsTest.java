/*
 * Copyright 2021 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UConcatLiterals}. */
@RunWith(JUnit4.class)
public class UConcatLiteralsTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UConcatLiterals.create(
                ImmutableList.of(
                    ULiteral.stringLit("foo"), ULiteral.stringLit("bar"), UFreeIdent.create("y"))))
        .addEqualityGroup(
            UConcatLiterals.create(
                ImmutableList.of(
                    ULiteral.stringLit("foo"), UFreeIdent.create("y"), ULiteral.stringLit("bar"))))
        .addEqualityGroup(
            UConcatLiterals.create(
                ImmutableList.of(
                    ULiteral.stringLit("foo"),
                    ULiteral.stringLit("bar"),
                    UFreeIdent.create("x"),
                    ULiteral.stringLit("baz"),
                    UFreeIdent.create("y"),
                    UFreeIdent.create("z"),
                    ULiteral.stringLit("foo2"),
                    ULiteral.intLit(5),
                    UFreeIdent.create("a"))))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo"),
                ULiteral.charLit('t'),
                UFreeIdent.create("x"),
                ULiteral.stringLit("baz"),
                UFreeIdent.create("y"),
                UFreeIdent.create("z"),
                ULiteral.stringLit("foo2"),
                ULiteral.booleanLit(true),
                ULiteral.intLit(5),
                UFreeIdent.create("a"))));
  }

  @Test
  public void inlineEmptyExpression() {
    UConcatLiterals tree = UConcatLiterals.create();

    assertInlines("\"\"", tree);
  }

  @Test
  public void inlineString() {
    UConcatLiterals tree = UConcatLiterals.create(ULiteral.stringLit("foo"));

    assertInlines("\"foo\"", tree);
  }

  @Test
  public void inlineTwoStrings() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(ULiteral.stringLit("foo"), ULiteral.stringLit("bar")));

    assertInlines("\"foobar\"", tree);
  }

  @Test
  public void inlineStringWithChar() {
    UConcatLiterals tree =
        UConcatLiterals.create(ImmutableList.of(ULiteral.stringLit("foo"), ULiteral.charLit('t')));

    assertInlines("\"foot\"", tree);
  }

  @Test
  public void inlineCharWithString() {
    UConcatLiterals tree =
        UConcatLiterals.create(ImmutableList.of(ULiteral.charLit('f'), ULiteral.stringLit("oot")));

    assertInlines("\"foot\"", tree);
  }

  @Test
  public void inlineStringWithInt() {
    UConcatLiterals tree =
        UConcatLiterals.create(ImmutableList.of(ULiteral.stringLit("foo "), ULiteral.intLit(5)));

    assertInlines("\"foo 5\"", tree);
  }

  @Test
  public void inlineWithIntWithString() {
    UConcatLiterals tree =
        UConcatLiterals.create(ImmutableList.of(ULiteral.intLit(6), ULiteral.stringLit("foo")));

    assertInlines("\"6foo\"", tree);
  }

  @Test
  public void inlineWithDoubleWithString() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(ULiteral.doubleLit(12.11), ULiteral.stringLit("foo")));

    assertInlines("\"12.11foo\"", tree);
  }

  @Test
  public void inlineWithLongWithString() {
    UConcatLiterals tree =
        UConcatLiterals.create(ImmutableList.of(ULiteral.longLit(102L), ULiteral.stringLit("foo")));

    assertInlines("\"102foo\"", tree);
  }

  @Test
  public void inlineWithBooleanInBetween() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo"), ULiteral.booleanLit(true), ULiteral.stringLit("bar")));

    assertInlines("\"footruebar\"", tree);
  }

  @Test
  public void inlineWithIdentInBetween() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo"), UFreeIdent.create("y"), ULiteral.stringLit("bar")));

    bind(new UFreeIdent.Key("y"), parseExpression("y"));

    assertInlines("\"foo\" + y + \"bar\"", tree);
  }

  @Test
  public void inlineWithTrailingIdent() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo"), ULiteral.stringLit("bar"), UFreeIdent.create("y")));

    bind(new UFreeIdent.Key("y"), parseExpression("y"));

    assertInlines("\"foobar\" + y", tree);
  }

  @Test
  public void inlineWithTrailingIdentLiteral() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo"), ULiteral.stringLit("bar"), UFreeIdent.create("y")));

    bind(new UFreeIdent.Key("y"), parseExpression("\"baz\""));

    assertInlines("\"foobarbaz\"", tree);
  }

  @Test
  public void inlineWithStringsAndIdents() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo"),
                ULiteral.stringLit("bar"),
                UFreeIdent.create("x"),
                ULiteral.stringLit("baz"),
                UFreeIdent.create("y"),
                UFreeIdent.create("z"),
                ULiteral.stringLit("foo2 "),
                ULiteral.stringLit("bar2 "),
                UFreeIdent.create("a")));

    bind(new UFreeIdent.Key("x"), parseExpression("x"));
    bind(new UFreeIdent.Key("y"), parseExpression("y"));
    bind(new UFreeIdent.Key("z"), parseExpression("z"));
    bind(new UFreeIdent.Key("a"), parseExpression("\"baz2\""));

    assertInlines("\"foobar\" + x + \"baz\" + y + z + \"foo2 bar2 baz2\"", tree);
  }

  @Test
  public void inlineLiteralsAndIdents() {
    UConcatLiterals tree =
        UConcatLiterals.create(
            ImmutableList.of(
                ULiteral.stringLit("foo "),
                ULiteral.stringLit("bar "),
                UFreeIdent.create("x"),
                ULiteral.stringLit(" baz "),
                ULiteral.charLit('t'),
                UFreeIdent.create("y"),
                ULiteral.intLit(5),
                ULiteral.stringLit("foo2 "),
                UFreeIdent.create("z")));

    bind(new UFreeIdent.Key("x"), parseExpression("false"));
    bind(new UFreeIdent.Key("y"), parseExpression("y"));
    bind(new UFreeIdent.Key("z"), parseExpression("\"bar2\""));

    assertInlines("\"foo bar false baz t\" + y + \"5foo2 bar2\"", tree);
  }
}
