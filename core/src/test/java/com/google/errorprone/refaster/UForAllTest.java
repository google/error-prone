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
import javax.lang.model.type.TypeKind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UForAll}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UForAllTest {
  @Test
  public void equality() {
    UType objectType = UClassType.create("java.lang.Object", ImmutableList.<UType>of());
    UTypeVar eType = UTypeVar.create("E", objectType);
    UTypeVar tType = UTypeVar.create("T", objectType);
    UType listOfEType = UClassType.create("java.util.List", ImmutableList.<UType>of(eType));
    new EqualsTester()
        .addEqualityGroup(UForAll.create(ImmutableList.of(eType), eType)) // <E> E
        .addEqualityGroup(UForAll.create(ImmutableList.of(eType), listOfEType)) // <E> List<E>
        .addEqualityGroup(UForAll.create(ImmutableList.of(tType), tType)) // <T> T
        .testEquals();
  }

  @Test
  public void serialization() {
    UType nullType = UPrimitiveType.create(TypeKind.NULL);
    UType objectType = UClassType.create("java.lang.Object", ImmutableList.<UType>of());
    UTypeVar eType = UTypeVar.create("E", nullType, objectType);
    UType listOfEType = UClassType.create("java.util.List", ImmutableList.<UType>of(eType));
    SerializableTester.reserializeAndAssert(UForAll.create(ImmutableList.of(eType), listOfEType));
  }
}
