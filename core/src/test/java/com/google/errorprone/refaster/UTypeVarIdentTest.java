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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.refaster.UTypeVar.TypeWithExpression;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UTypeVarIdent}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UTypeVarIdentTest extends AbstractUTreeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(UTypeVarIdent.create("E"))
        .addEqualityGroup(UTypeVarIdent.create("T"))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(UTypeVarIdent.create("E"));
  }

  @Test
  public void inline() {
    ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    Symtab symtab = Symtab.instance(context);
    Type listType = symtab.listType;
    bind(
        new UTypeVar.Key("E"),
        TypeWithExpression.create(
            new ClassType(listType, List.<Type>of(symtab.stringType), listType.tsym)));
    assertInlines("List<String>", UTypeVarIdent.create("E"));
    assertEquals(ImmutableSet.of("java.util.List"), inliner.getImportsToAdd());
  }
}
