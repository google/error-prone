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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UNewClass}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UNewClassTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UNewClass.create(UClassIdent.create("java.lang.String"), ULiteral.stringLit("123")))
        .addEqualityGroup(
            UNewClass.create(UClassIdent.create("java.math.BigInteger"), ULiteral.stringLit("123")))
        .addEqualityGroup(
            UNewClass.create(UClassIdent.create("java.lang.String"), ULiteral.stringLit("foobar")))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UNewClass.create(UClassIdent.create("java.lang.String"), ULiteral.stringLit("123")));
  }

  @Test
  public void inline() {
    ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    assertInlines(
        "new String(\"123\")",
        UNewClass.create(UClassIdent.create("java.lang.String"), ULiteral.stringLit("123")));
  }
}
