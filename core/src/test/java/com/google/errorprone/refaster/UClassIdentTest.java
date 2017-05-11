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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symtab;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UClassIdent}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UClassIdentTest extends AbstractUTreeTest {
  @Test
  public void equality() throws CouldNotResolveImportException {
    new EqualsTester()
        .addEqualityGroup(UClassIdent.create("java.util.List"))
        .addEqualityGroup(UClassIdent.create("com.sun.tools.javac.util.List"))
        .addEqualityGroup(
            UClassIdent.create("java.lang.String"),
            UClassIdent.create(inliner.resolveClass("java.lang.String")))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(UClassIdent.create("java.math.BigInteger"));
  }

  @Test
  public void inline() {
    ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    context.put(PackageSymbol.class, Symtab.instance(context).rootPackage);
    assertInlines("List", UClassIdent.create("java.util.List"));
    assertInlines("Map.Entry", UClassIdent.create("java.util.Map.Entry"));
  }

  @Test
  public void importConflicts() {
    ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    context.put(PackageSymbol.class, Symtab.instance(context).rootPackage);
    // Test fully qualified class names
    inliner.addImport("package.Exception");
    assertInlines("Exception", UClassIdent.create("package.Exception"));
    // Will import "anotherPackage.Exception" due to conflicts
    assertInlines("anotherPackage.Exception", UClassIdent.create("anotherPackage.Exception"));
    new EqualsTester()
        .addEqualityGroup(inliner.getImportsToAdd(), ImmutableSet.of("package.Exception"))
        .testEquals();
    // Test nested class names
    inliner.addImport("package.subpackage.Foo.Bar");
    // Will import "package.Foo"
    assertInlines("Foo.Bar", UClassIdent.create("package.Foo.Bar"));
    assertInlines("Bar", UClassIdent.create("package.subpackage.Foo.Bar"));
    // Will not import "anotherPackage.Foo" due to conflicts
    assertInlines("anotherPackage.Foo.Bar", UClassIdent.create("anotherPackage.Foo.Bar"));
    new EqualsTester()
        .addEqualityGroup(
            inliner.getImportsToAdd(),
            ImmutableSet.of("package.Exception", "package.subpackage.Foo.Bar", "package.Foo"))
        .testEquals();
  }
}
