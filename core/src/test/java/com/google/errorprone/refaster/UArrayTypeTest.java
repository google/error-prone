/*
 * Copyright 2013 The Error Prone Authors.
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
 * Tests for {@link UArrayType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UArrayTypeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(UArrayType.create(UPrimitiveType.INT))
        .addEqualityGroup(UArrayType.create(UClassType.create("java.lang.String")))
        .addEqualityGroup(UArrayType.create(UArrayType.create(UPrimitiveType.INT)))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UArrayType.create(UClassType.create("java.lang.String")));
    SerializableTester.reserializeAndAssert(UArrayType.create(UPrimitiveType.INT));
    SerializableTester.reserializeAndAssert(
        UArrayType.create(UArrayType.create(UPrimitiveType.INT)));
  }
}
