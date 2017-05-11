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
 * Tests for {@link UTypeVar}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UTypeVarTest {
  @Test
  public void equality() {
    UType nullType = UPrimitiveType.create(TypeKind.NULL);
    UType objectType = UClassType.create("java.lang.Object", ImmutableList.<UType>of());
    UType charSequenceType = UClassType.create("java.lang.CharSequence", ImmutableList.<UType>of());
    UType stringType = UClassType.create("java.lang.String", ImmutableList.<UType>of());

    new EqualsTester()
        .addEqualityGroup(UTypeVar.create("T", nullType, charSequenceType))
        // T extends CharSequence
        .addEqualityGroup(UTypeVar.create("T", stringType, charSequenceType))
        // T extends CharSequence super String
        .addEqualityGroup(UTypeVar.create("T", nullType, objectType))
        // T extends Object
        .addEqualityGroup(UTypeVar.create("E", nullType, charSequenceType))
        // E extends CharSequence
        .testEquals();
  }

  @Test
  public void serialization() {
    UType nullType = UPrimitiveType.create(TypeKind.NULL);
    UType charSequenceType = UClassType.create("java.lang.CharSequence", ImmutableList.<UType>of());
    SerializableTester.reserializeAndAssert(UTypeVar.create("T", nullType, charSequenceType));
  }
}
