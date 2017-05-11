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

import static org.junit.Assert.assertNotNull;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.tools.javac.code.Symtab;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UClassType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UClassTypeTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    UType stringType = UClassType.create("java.lang.String");
    new EqualsTester()
        .addEqualityGroup(stringType)
        .addEqualityGroup(UClassType.create("java.util.List", stringType))
        .addEqualityGroup(UClassType.create("java.util.Map", stringType, stringType))
        .addEqualityGroup(UClassType.create("java.lang.Integer"))
        .addEqualityGroup(
            UClassType.create("java.util.List", UClassType.create("java.lang.Integer")))
        .testEquals();
  }

  @Test
  public void serialization() {
    UClassType stringType = UClassType.create("java.lang.String");
    UClassType integerType = UClassType.create("java.lang.Integer");
    SerializableTester.reserializeAndAssert(stringType);
    SerializableTester.reserializeAndAssert(integerType);
    SerializableTester.reserializeAndAssert(UClassType.create("java.util.List", stringType));
    SerializableTester.reserializeAndAssert(UClassType.create("java.util.List", integerType));
    SerializableTester.reserializeAndAssert(
        UClassType.create("java.util.Map", stringType, stringType));
  }

  @Test
  public void unifies() {
    assertNotNull(
        UClassType.create("java.lang.String").unify(Symtab.instance(context).stringType, unifier));
  }
}
