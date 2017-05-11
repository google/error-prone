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
import com.sun.tools.javac.code.BoundKind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UWildcardType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UWildcardTypeTest {
  @Test
  public void equality() {
    UType objectType = UClassType.create("java.lang.Object", ImmutableList.<UType>of());
    UType setType = UClassType.create("java.util.Set", ImmutableList.<UType>of(objectType));

    new EqualsTester()
        .addEqualityGroup(UWildcardType.create(BoundKind.UNBOUND, objectType)) // ?
        .addEqualityGroup(UWildcardType.create(BoundKind.EXTENDS, objectType)) // ? extends Object
        .addEqualityGroup(UWildcardType.create(BoundKind.EXTENDS, setType)) // ? extends Set<Object>
        .addEqualityGroup(UWildcardType.create(BoundKind.SUPER, setType)) // ? super Set<Object>
        .testEquals();
  }

  @Test
  public void serialization() {
    UType numberType = UClassType.create("java.lang.Number", ImmutableList.<UType>of());
    SerializableTester.reserializeAndAssert(UWildcardType.create(BoundKind.EXTENDS, numberType));
  }
}
