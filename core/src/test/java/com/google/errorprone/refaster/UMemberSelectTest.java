/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.refaster.UTypeVar.TypeWithExpression;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UMemberSelect}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UMemberSelectTest extends AbstractUTreeTest {
  @Test
  public void inline() {
    ULiteral fooLit = ULiteral.stringLit("foo");
    UType type = mock(UType.class);
    UMemberSelect memberSelect = UMemberSelect.create(fooLit, "length", type);
    assertInlines("\"foo\".length", memberSelect);
  }

  @Test
  public void inlineClassStripsTypeArguments() throws CouldNotResolveImportException {
    UType type = mock(UType.class);
    // TODO(b/583393246): should be "List.class"
    assertInlines(
        "List<String>.class",
        UMemberSelect.create(
            UTypeApply.create(
                UClassIdent.create("java.util.List"), UClassIdent.create("java.lang.String")),
            "class",
            type));

    TreeMaker maker = TreeMaker.instance(context);
    Names names = Names.instance(context);
    JCExpression wildcard = maker.Wildcard(maker.TypeBoundKind(BoundKind.UNBOUND), null);
    JCExpression outerWildcard =
        maker.TypeApply(maker.Ident(names.fromString("Outer")), List.of(wildcard));
    JCExpression outerInner = maker.Select(outerWildcard, names.fromString("Inner"));

    UTypeVar.Key key = new UTypeVar.Key(StringName.of("T"));
    UMemberSelect typeVarClass = UMemberSelect.create(UTypeVarIdent.create("T"), "class", type);

    bind(key, TypeWithExpression.create(Symtab.instance(context).objectType, outerInner));
    JCFieldAccess inlinedOuterInner = (JCFieldAccess) typeVarClass.inline(inliner);
    // TODO(b/583393246): should be "Outer.Inner.class"
    assertThat(inlinedOuterInner.toString()).isEqualTo("Outer<?>.Inner.class");
    // TODO(b/583393246): should be isNotSameInstanceAs(outerInner)
    assertThat(inlinedOuterInner.selected).isSameInstanceAs(outerInner);

    JCExpression annotatedArray =
        maker.TypeArray(
            maker.AnnotatedType(
                List.of(maker.Annotation(maker.Ident(names.fromString("Nullable")), List.nil())),
                maker.TypeApply(maker.Ident(names.fromString("List")), List.of(wildcard))));
    bind(key, TypeWithExpression.create(Symtab.instance(context).objectType, annotatedArray));
    // TODO(b/583393246): should be "List[].class"
    assertInlines("@Nullable List<?>[].class", typeVarClass);
  }

  @Test
  public void equality() {
    UType stringTy = UClassType.create("java.lang.String");

    // int String.indexOf(int)
    UMethodType indexOfIntTy = UMethodType.create(UPrimitiveType.INT, UPrimitiveType.INT);
    // int String.indexOf(String)
    UMethodType indexOfStringTy = UMethodType.create(UPrimitiveType.INT, stringTy);

    UExpression fooLit = ULiteral.stringLit("foo");
    UExpression barLit = ULiteral.stringLit("bar");

    new EqualsTester()
        .addEqualityGroup(UMemberSelect.create(fooLit, "indexOf", indexOfIntTy))
        .addEqualityGroup(UMemberSelect.create(fooLit, "indexOf", indexOfStringTy))
        .addEqualityGroup(UMemberSelect.create(barLit, "indexOf", indexOfIntTy))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UMemberSelect.create(
            ULiteral.stringLit("foo"),
            "indexOf",
            UMethodType.create(UPrimitiveType.INT, UPrimitiveType.INT)));
  }

  @Test
  public void unifyWithModuleIdentifier() {
    UMemberSelect memberSelect =
        UMemberSelect.create(ULiteral.stringLit("foo"), "length", mock(UType.class));

    ModuleSymbol module =
        Symtab.instance(context).enterModule(Names.instance(context).fromString("somemodule"));
    JCIdent moduleIdentifier = TreeMaker.instance(context).Ident(module);

    assertThat(memberSelect.unify(moduleIdentifier, unifier).findFirst()).isEmpty();
  }
}
