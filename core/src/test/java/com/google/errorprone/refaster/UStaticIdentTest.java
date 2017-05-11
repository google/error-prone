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
 * Tests for {@link UStaticIdent}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UStaticIdentTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UStaticIdent.create(
                "java.lang.Integer",
                "valueOf",
                UMethodType.create(
                    UClassType.create("java.lang.Integer"), UClassType.create("java.lang.String"))))
        .addEqualityGroup(
            UStaticIdent.create(
                "java.lang.Integer",
                "valueOf",
                UMethodType.create(
                    UClassType.create("java.lang.Integer"),
                    UClassType.create("java.lang.String"),
                    UPrimitiveType.INT)))
        .addEqualityGroup(
            UStaticIdent.create(
                "java.lang.Integer",
                "getInteger",
                UMethodType.create(
                    UClassType.create("java.lang.Integer"), UClassType.create("java.lang.String"))))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UStaticIdent.create(
            "java.lang.Integer",
            "valueOf",
            UMethodType.create(
                UClassType.create("java.lang.Integer"), UClassType.create("java.lang.String"))));
  }

  @Test
  public void inline() {
    ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    assertInlines(
        "Integer.valueOf",
        UStaticIdent.create(
            "java.lang.Integer",
            "valueOf",
            UMethodType.create(
                UClassType.create("java.lang.Integer"), UClassType.create("java.lang.String"))));
  }
}
