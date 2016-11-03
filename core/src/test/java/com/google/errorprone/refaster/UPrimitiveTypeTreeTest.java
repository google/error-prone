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
import com.sun.tools.javac.code.TypeTag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UPrimitiveType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UPrimitiveTypeTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.INT), UPrimitiveTypeTree.INT)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.LONG), UPrimitiveTypeTree.LONG)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.DOUBLE), UPrimitiveTypeTree.DOUBLE)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.FLOAT), UPrimitiveTypeTree.FLOAT)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.CHAR), UPrimitiveTypeTree.CHAR)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.VOID), UPrimitiveTypeTree.VOID)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.BOT), UPrimitiveTypeTree.NULL)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.BOOLEAN), UPrimitiveTypeTree.BOOLEAN)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.BYTE), UPrimitiveTypeTree.BYTE)
        .addEqualityGroup(UPrimitiveTypeTree.create(TypeTag.SHORT), UPrimitiveTypeTree.SHORT)
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(UPrimitiveTypeTree.INT);
  }
}
