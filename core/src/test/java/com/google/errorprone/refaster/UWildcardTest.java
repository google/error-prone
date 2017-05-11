/*
 * Copyright 2014 Google Inc. All rights reserved.
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
import com.sun.source.tree.Tree.Kind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UWildcard}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UWildcardTest {
  @Test
  public void equality() {
    UExpression objectIdent = UClassIdent.create("java.lang.Object");
    UExpression setIdent = UTypeApply.create("java.util.Set", objectIdent);

    new EqualsTester()
        .addEqualityGroup(UWildcard.create(Kind.UNBOUNDED_WILDCARD, null))
        .addEqualityGroup(UWildcard.create(Kind.EXTENDS_WILDCARD, objectIdent))
        .addEqualityGroup(UWildcard.create(Kind.EXTENDS_WILDCARD, setIdent))
        // ? extends Set<Object>
        .addEqualityGroup(UWildcard.create(Kind.SUPER_WILDCARD, setIdent))
        // ? super Set<Object>
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UWildcard.create(Kind.EXTENDS_WILDCARD, UClassIdent.create("java.lang.Number")));
  }
}
