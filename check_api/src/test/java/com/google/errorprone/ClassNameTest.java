/*
 * Copyright 2026 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import org.junit.Test;

/** Tests for {@link ClassName}. */
public final class ClassNameTest {

  @Test
  public void validation() {
    assertThrows(NullPointerException.class, () -> ClassName.of(null));
    assertThrows(IllegalArgumentException.class, () -> ClassName.of(""));
    assertThrows(IllegalArgumentException.class, () -> ClassName.of("com.google.Foo."));
    assertThrows(IllegalArgumentException.class, () -> ClassName.of("java.lang.String[]"));
    assertThrows(IllegalArgumentException.class, () -> ClassName.of("java.util.List<String>"));
    for (String primitive :
        ImmutableList.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void")) {
      assertThrows(IllegalArgumentException.class, () -> ClassName.of(primitive));
    }
  }

  @Test
  public void equalsHashCodeAndToString() {
    new EqualsTester()
        .addEqualityGroup(ClassName.of("java.util.Map.Entry"), ClassName.of("java.util.Map$Entry"))
        .addEqualityGroup(ClassName.of("java.util.List"))
        .testEquals();
    assertThat(ClassName.of("java.util.Map.Entry").toString()).isEqualTo("java.util.Map$Entry");
    assertThat(ClassName.of("java.util.List").toString()).isEqualTo("java.util.List");
  }

  @Test
  public void resolveTopLevelAndNestedClasses() {
    Context context = newContext();
    VisitorState state = VisitorState.createForUtilityPurposes(context);
    Symtab symtab = state.getSymtab();
    var unused1 = symtab.enterClass(symtab.java_base, state.getNames().fromString("java.util.Map"));
    var unused2 =
        symtab.enterClass(
            symtab.java_base, state.getNames().fromString("java.lang.invoke.MethodHandles"));

    ClassName objectName = ClassName.of("java.lang.Object");
    ClassSymbol objectSym = objectName.symbol(state);
    assertThat(objectSym).isNotNull();
    assertThat(objectName.type(state)).isSameInstanceAs(objectSym.asType());
    assertThat(objectName.asTypeSupplier().get(state)).isSameInstanceAs(objectSym.asType());

    // Map is entered in Symtab.classes but not yet completed, so Map$Entry is not yet in
    // Symtab.classes; ClassName.symbol(state) should complete Map and resolve Map$Entry.
    assertThat(
            symtab.getClass(symtab.java_base, state.getNames().fromString("java.util.Map$Entry")))
        .isNull();
    ClassName mapEntryCanonical = ClassName.of("java.util.Map.Entry");
    ClassName mapEntryBinary = ClassName.of("java.util.Map$Entry");
    ClassSymbol entrySym = mapEntryCanonical.symbol(state);
    assertThat(entrySym).isNotNull();
    assertThat(mapEntryBinary.symbol(state)).isSameInstanceAs(entrySym);
    assertThat(mapEntryCanonical.binaryName(state).toString()).isEqualTo("java.util.Map$Entry");

    // Multi-level nested class (MethodHandles -> Lookup -> ClassOption) when only outer is entered:
    ClassName classOption = ClassName.of("java.lang.invoke.MethodHandles.Lookup.ClassOption");
    assertThat(classOption.symbol(state)).isNotNull();

    // Missing top-level class (no '$') and non-existent nested class under an existing outer class:
    ClassName missingTopLevel = ClassName.of("com.example.DoesNotExist");
    assertThat(missingTopLevel.symbol(state)).isNull();
    assertThat(missingTopLevel.type(state)).isNull();
    assertThat(missingTopLevel.asTypeSupplier().get(state)).isNull();

    ClassName missingNested = ClassName.of("java.util.Map.NonExistent");
    assertThat(missingNested.symbol(state)).isNull();
    assertThat(missingNested.type(state)).isNull();
  }

  @Test
  public void cachingAcrossUnitsAndInvocations() {
    ClassName spliteratorOfInt = ClassName.of("java.util.Spliterator.OfInt");

    Context context1 = newContext();
    VisitorState unit1State = VisitorState.createForUtilityPurposes(context1);
    // Not entered yet in unit 1:
    assertThat(spliteratorOfInt.symbol(unit1State)).isNull();
    assertThat(spliteratorOfInt.type(unit1State)).isNull();
    assertThat(spliteratorOfInt.asTypeSupplier().get(unit1State)).isNull();

    // Enter Spliterator in Symtab; unit1State keeps its per-unit negative cache, whereas
    // unit2State (a fresh compilation unit in the same javac invocation) resolves it:
    Symtab symtab1 = unit1State.getSymtab();
    var unused1 =
        symtab1.enterClass(
            symtab1.java_base, unit1State.getNames().fromString("java.util.Spliterator"));
    assertThat(spliteratorOfInt.symbol(unit1State)).isNull();

    VisitorState unit2State = VisitorState.createForUtilityPurposes(context1);
    ClassSymbol sym1 = spliteratorOfInt.symbol(unit2State);
    assertThat(sym1).isNotNull();
    assertThat(spliteratorOfInt.type(unit2State)).isSameInstanceAs(sym1.asType());

    // Subsequent compilation invocation gets a fresh symbol from its own Context:
    Context context2 = newContext();
    VisitorState invocation2State = VisitorState.createForUtilityPurposes(context2);
    Symtab symtab2 = invocation2State.getSymtab();
    var unused2 =
        symtab2.enterClass(
            symtab2.java_base, invocation2State.getNames().fromString("java.util.Spliterator"));
    ClassSymbol sym2 = spliteratorOfInt.symbol(invocation2State);
    assertThat(sym2).isNotNull();
    assertThat(sym2).isNotSameInstanceAs(sym1);
  }

  private static Context newContext() {
    JavacTask task =
        JavacTool.create()
            .getTask(
                /* out= */ null,
                new JavacFileManager(new Context(), /* register= */ false, UTF_8),
                /* diagnosticListener= */ null,
                /* options= */ ImmutableList.of(),
                /* classes= */ ImmutableList.of(),
                /* compilationUnits= */ ImmutableList.of());
    ((JavacTaskImpl) task).ensureEntered();
    return ((BasicJavacTask) task).getContext();
  }
}
