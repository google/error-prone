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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link URepeated}. */
@RunWith(JUnit4.class)
public class URepeatedTest extends AbstractUTreeTest {

  @Test
  public void unifies() {
    JCExpression expr = parseExpression("\"abcdefg\".charAt(x + 1)");
    URepeated ident = URepeated.create("foo", UFreeIdent.create("foo"));
    assertNotNull(ident.unify(expr, unifier));
    assertEquals(ImmutableMap.of(new UFreeIdent.Key("foo"), expr), unifier.getBindings());
  }

  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(URepeated.create("foo", UFreeIdent.create("foo")))
        .addEqualityGroup(URepeated.create("bar", UFreeIdent.create("bar")))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(URepeated.create("foo", UFreeIdent.create("foo")));
  }
}
