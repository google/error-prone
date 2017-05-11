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

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import java.lang.annotation.Annotation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UTemplater#createTemplate} against real compiled code.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class TemplatingTest extends CompilerBasedTest {

  @Test
  public void arrayAccess() {
    compile(
        "class ArrayAccessExample {",
        "  public double example(double[] array) {",
        "    return array[5];",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("array", UArrayType.create(UPrimitiveType.DOUBLE)),
            UArrayAccess.create(UFreeIdent.create("array"), ULiteral.intLit(5)),
            UPrimitiveType.DOUBLE),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void binary() {
    compile(
        "class BinaryExample {",
        "  public int example(int x, int y) {",
        "    return (x + y) / 2;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of(
                "x", UPrimitiveType.INT,
                "y", UPrimitiveType.INT),
            UBinary.create(
                Kind.DIVIDE,
                UParens.create(
                    UBinary.create(Kind.PLUS, UFreeIdent.create("x"), UFreeIdent.create("y"))),
                ULiteral.intLit(2)),
            UPrimitiveType.INT),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void conditional() {
    compile(
        "class ConditionalExample {",
        "  public String example(boolean foo) {",
        "    return foo ? null : \"bar\";",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("foo", UPrimitiveType.BOOLEAN),
            UConditional.create(
                UFreeIdent.create("foo"), ULiteral.nullLit(), ULiteral.stringLit("bar")),
            UClassType.create("java.lang.String")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void whileLoop() {
    compile(
        "import java.util.Iterator;",
        "class WhileLoopExample {",
        "  public void example(Iterator<?> itr) {",
        "    while (itr.hasNext()) {",
        "      itr.next();",
        "    }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of("itr", UClassType.create("java.util.Iterator", UWildcardType.create())),
            UWhileLoop.create(
                UParens.create(
                    UMethodInvocation.create(
                        UMemberSelect.create(
                            UFreeIdent.create("itr"),
                            "hasNext",
                            UMethodType.create(UPrimitiveType.BOOLEAN)))),
                UBlock.create(
                    UExpressionStatement.create(
                        UMethodInvocation.create(
                            UMemberSelect.create(
                                UFreeIdent.create("itr"),
                                "next",
                                UMethodType.create(UTypeVar.create("E")))))))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void fullyQualifiedGlobalIdent() {
    compile(
        "class FullyQualifiedIdentExample {",
        "  public java.math.RoundingMode example() {",
        "    return java.math.RoundingMode.FLOOR;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            UStaticIdent.create(
                "java.math.RoundingMode", "FLOOR", UClassType.create("java.math.RoundingMode")),
            UClassType.create("java.math.RoundingMode")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void qualifiedGlobalIdent() {
    compile(
        "import java.math.RoundingMode;",
        "class QualifiedIdentExample {",
        "  public RoundingMode example() {",
        "    return RoundingMode.FLOOR;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            UStaticIdent.create(
                "java.math.RoundingMode", "FLOOR", UClassType.create("java.math.RoundingMode")),
            UClassType.create("java.math.RoundingMode")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void staticImportedGlobalIdent() {
    compile(
        "import java.math.RoundingMode;",
        "import static java.math.RoundingMode.FLOOR;",
        "class StaticImportedIdentExample {",
        "  public RoundingMode example() {",
        "    return FLOOR;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            UStaticIdent.create(
                "java.math.RoundingMode", "FLOOR", UClassType.create("java.math.RoundingMode")),
            UClassType.create("java.math.RoundingMode")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void staticMethodInvocation() {
    compile(
        "import java.math.BigInteger;",
        "class StaticMethodExample {",
        "  public BigInteger example(int x) {",
        "    return BigInteger.valueOf(x);",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("x", UPrimitiveType.INT),
            UMethodInvocation.create(
                UStaticIdent.create(
                    "java.math.BigInteger",
                    "valueOf",
                    UMethodType.create(
                        UClassType.create("java.math.BigInteger"), UPrimitiveType.LONG)),
                UFreeIdent.create("x")),
            UClassType.create("java.math.BigInteger")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void instanceMethodInvocation() {
    compile(
        "class InstanceMethodExample {",
        "  public char example(String str) {",
        "    return str.charAt(5);",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("str", UClassType.create("java.lang.String")),
            UMethodInvocation.create(
                UMemberSelect.create(
                    UFreeIdent.create("str"),
                    "charAt",
                    UMethodType.create(UPrimitiveType.CHAR, UPrimitiveType.INT)),
                ULiteral.intLit(5)),
            UPrimitiveType.CHAR),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void anyOf() {
    compile(
        "import com.google.errorprone.refaster.Refaster;",
        "class InstanceMethodExample {",
        "  public char example(String str) {",
        "    return str.charAt(Refaster.anyOf(1, 3, 5));",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("str", UClassType.create("java.lang.String")),
            UMethodInvocation.create(
                UMemberSelect.create(
                    UFreeIdent.create("str"),
                    "charAt",
                    UMethodType.create(UPrimitiveType.CHAR, UPrimitiveType.INT)),
                UAnyOf.create(ULiteral.intLit(1), ULiteral.intLit(3), ULiteral.intLit(5))),
            UPrimitiveType.CHAR),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void unary() {
    compile(
        "import java.util.Arrays;",
        "class UnaryExample {",
        "  public int example(int[] array, int key) {",
        "    return ~Arrays.binarySearch(array, key);",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of(
                "array", UArrayType.create(UPrimitiveType.INT), "key", UPrimitiveType.INT),
            UUnary.create(
                Kind.BITWISE_COMPLEMENT,
                UMethodInvocation.create(
                    UStaticIdent.create(
                        "java.util.Arrays",
                        "binarySearch",
                        UMethodType.create(
                            UPrimitiveType.INT,
                            UArrayType.create(UPrimitiveType.INT),
                            UPrimitiveType.INT)),
                    UFreeIdent.create("array"),
                    UFreeIdent.create("key"))),
            UPrimitiveType.INT),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void genericTemplate() {
    compile(
        "import java.util.List;",
        "class GenericTemplateExample {",
        "  public <E> E example(List<E> list) {",
        "    return list.get(0);",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableClassToInstanceMap.<Annotation>builder().build(),
            ImmutableList.of(UTypeVar.create("E")),
            ImmutableMap.of("list", UClassType.create("java.util.List", UTypeVar.create("E"))),
            UMethodInvocation.create(
                UMemberSelect.create(
                    UFreeIdent.create("list"),
                    "get",
                    UMethodType.create(UTypeVar.create("E"), UPrimitiveType.INT)),
                ULiteral.intLit(0)),
            UTypeVar.create("E")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void genericMethodInvocation() {
    compile(
        "import java.util.Collections;",
        "import java.util.List;",
        "class GenericTemplateExample {",
        "  public <E> List<E> example(List<E> list) {",
        "    return Collections.unmodifiableList(list);",
        "  }",
        "}");
    UTypeVar tVar = UTypeVar.create("T");
    UTypeVar eVar = UTypeVar.create("E");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableClassToInstanceMap.<Annotation>builder().build(),
            ImmutableList.of(eVar),
            ImmutableMap.of("list", UClassType.create("java.util.List", eVar)),
            UMethodInvocation.create(
                UStaticIdent.create(
                    "java.util.Collections",
                    "unmodifiableList",
                    UForAll.create(
                        ImmutableList.of(tVar),
                        UMethodType.create(
                            UClassType.create("java.util.List", tVar),
                            UClassType.create(
                                "java.util.List", UWildcardType.create(BoundKind.EXTENDS, tVar))))),
                UFreeIdent.create("list")),
            UClassType.create("java.util.List", eVar)),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void doWhile() {
    compile(
        "class DoWhileExample {",
        "  public void example(String str) {",
        "    int old = 0;",
        "    do {",
        "      old = str.indexOf(' ', old + 1);",
        "    } while (old != -1);",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of("str", UClassType.create("java.lang.String")),
            UVariableDecl.create("old", UPrimitiveTypeTree.INT, ULiteral.intLit(0)),
            UDoWhileLoop.create(
                UBlock.create(
                    UExpressionStatement.create(
                        UAssign.create(
                            ULocalVarIdent.create("old"),
                            UMethodInvocation.create(
                                UMemberSelect.create(
                                    UFreeIdent.create("str"),
                                    "indexOf",
                                    UMethodType.create(
                                        UPrimitiveType.INT,
                                        UPrimitiveType.INT,
                                        UPrimitiveType.INT)),
                                ULiteral.charLit(' '),
                                UBinary.create(
                                    Kind.PLUS,
                                    ULocalVarIdent.create("old"),
                                    ULiteral.intLit(1)))))),
                UParens.create(
                    UBinary.create(
                        Kind.NOT_EQUAL_TO, ULocalVarIdent.create("old"), ULiteral.intLit(-1))))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void voidReturn() {
    compile(
        "import java.security.MessageDigest;",
        "class VoidExample {",
        "  public void example(MessageDigest md, byte[] bytes) {",
        "    md.update(bytes);",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of(
                "md", UClassType.create("java.security.MessageDigest"),
                "bytes", UArrayType.create(UPrimitiveType.BYTE)),
            UExpressionStatement.create(
                UMethodInvocation.create(
                    UMemberSelect.create(
                        UFreeIdent.create("md"),
                        "update",
                        UMethodType.create(
                            UPrimitiveType.VOID, UArrayType.create(UPrimitiveType.BYTE))),
                    UFreeIdent.create("bytes")))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void arrayCast() {
    compile(
        "class ArrayCastExample {",
        "  public String[] example(Object o) {",
        "    return (String[]) o;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("o", UClassType.create("java.lang.Object")),
            UTypeCast.create(
                UArrayTypeTree.create(UClassIdent.create("java.lang.String")),
                UFreeIdent.create("o")),
            UArrayType.create(UClassType.create("java.lang.String"))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void primitiveCast() {
    compile(
        "class PrimitiveCastExample {",
        "  public char example(int x) {",
        "    return (char) x;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("x", UPrimitiveType.INT),
            UTypeCast.create(UPrimitiveTypeTree.CHAR, UFreeIdent.create("x")),
            UPrimitiveType.CHAR),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void objectCast() {
    compile(
        "class ObjectCastExample {",
        "  public String example(Object o) {",
        "    return (String) o;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("o", UClassType.create("java.lang.Object")),
            UTypeCast.create(UClassIdent.create("java.lang.String"), UFreeIdent.create("o")),
            UClassType.create("java.lang.String")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void constructor() {
    compile(
        "import java.util.ArrayList;",
        "class ConstructorExample {",
        "  public String example() {",
        "    return new String(\"foo\");",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            UNewClass.create(UClassIdent.create("java.lang.String"), ULiteral.stringLit("foo")),
            UClassType.create("java.lang.String")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void forLoop() {
    compile(
        "class ForLoopExample {",
        "  public void example(int from, int to) {",
        "     for (int i = from; i < to; i++) {",
        "     }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of(
                "from", UPrimitiveType.INT,
                "to", UPrimitiveType.INT),
            UForLoop.create(
                ImmutableList.of(
                    UVariableDecl.create("i", UPrimitiveTypeTree.INT, UFreeIdent.create("from"))),
                UBinary.create(Kind.LESS_THAN, ULocalVarIdent.create("i"), UFreeIdent.create("to")),
                ImmutableList.of(
                    UExpressionStatement.create(
                        UUnary.create(Kind.POSTFIX_INCREMENT, ULocalVarIdent.create("i")))),
                UBlock.create())),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void forLoopNoStep() {
    compile(
        "class ForLoopExample {",
        "  public void example(int from, int to) {",
        "     for (int i = from; i < to;) {",
        "     }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of(
                "from", UPrimitiveType.INT,
                "to", UPrimitiveType.INT),
            UForLoop.create(
                ImmutableList.of(
                    UVariableDecl.create("i", UPrimitiveTypeTree.INT, UFreeIdent.create("from"))),
                UBinary.create(Kind.LESS_THAN, ULocalVarIdent.create("i"), UFreeIdent.create("to")),
                ImmutableList.<UExpressionStatement>of(),
                UBlock.create())),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void forLoopNoCondition() {
    compile(
        "class ForLoopExample {",
        "  public void example(int from, int to) {",
        "     for (int i = from; ; i++) {",
        "     }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of(
                "from", UPrimitiveType.INT,
                "to", UPrimitiveType.INT),
            UForLoop.create(
                ImmutableList.of(
                    UVariableDecl.create("i", UPrimitiveTypeTree.INT, UFreeIdent.create("from"))),
                null,
                ImmutableList.of(
                    UExpressionStatement.create(
                        UUnary.create(Kind.POSTFIX_INCREMENT, ULocalVarIdent.create("i")))),
                UBlock.create())),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void forLoopNoStart() {
    compile(
        "class ForLoopExample {",
        "  public void example(int from, int to) {",
        "     for (; from < to; from++) {",
        "     }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of(
                "from", UPrimitiveType.INT,
                "to", UPrimitiveType.INT),
            UForLoop.create(
                ImmutableList.<UStatement>of(),
                UBinary.create(Kind.LESS_THAN, UFreeIdent.create("from"), UFreeIdent.create("to")),
                ImmutableList.of(
                    UExpressionStatement.create(
                        UUnary.create(Kind.POSTFIX_INCREMENT, UFreeIdent.create("from")))),
                UBlock.create())),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void parameterizedCast() {
    compile(
        "import java.util.Collection;",
        "import java.util.List;",
        "class ParameterizedCastExample {",
        "  public List<String> example(Collection<String> elements) {",
        "    return (List<String>) elements;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of(
                "elements",
                UClassType.create("java.util.Collection", UClassType.create("java.lang.String"))),
            UTypeCast.create(
                UTypeApply.create(
                    UClassIdent.create("java.util.List"), UClassIdent.create("java.lang.String")),
                UFreeIdent.create("elements")),
            UClassType.create("java.util.List", UClassType.create("java.lang.String"))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void instanceOf() {
    compile(
        "class InstanceOfExample {",
        "  public boolean example(Object foo) {",
        "    return foo instanceof CharSequence;",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("foo", UClassType.create("java.lang.Object")),
            UInstanceOf.create(
                UFreeIdent.create("foo"), UClassIdent.create("java.lang.CharSequence")),
            UPrimitiveType.BOOLEAN),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void assignment() {
    compile(
        "class AssignmentExample {",
        "  public String example(String foo) {",
        "    return foo = \"bar\";",
        "  }",
        "}");
    assertEquals(
        ExpressionTemplate.create(
            ImmutableMap.of("foo", UClassType.create("java.lang.String")),
            UAssign.create(UFreeIdent.create("foo"), ULiteral.stringLit("bar")),
            UClassType.create("java.lang.String")),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void recursiveTypes() {
    compile(
        "class RecursiveTypeExample {",
        "  public <E extends Enum<E>> E example(E e) {",
        "    return e;",
        "  }",
        "}");
    Template<?> template = UTemplater.createTemplate(context, getMethodDeclaration("example"));
    UTypeVar eVar = Iterables.getOnlyElement(template.templateTypeVariables());
    assertEquals(
        ExpressionTemplate.create(
            ImmutableClassToInstanceMap.<Annotation>builder().build(),
            ImmutableList.of(UTypeVar.create("E", UClassType.create("java.lang.Enum", eVar))),
            ImmutableMap.of("e", eVar),
            UFreeIdent.create("e"),
            eVar),
        template);
  }

  @Test
  public void localVariable() {
    compile(
        "import java.util.ArrayList;",
        "import java.util.Comparator;",
        "import java.util.Collection;",
        "import java.util.Collections;",
        "import java.util.List;",
        "class LocalVariableExample {",
        "  public <E> void example(Collection<E> collection, Comparator<? super E> comparator) {",
        "    List<E> list = new ArrayList<E>(collection);",
        "    Collections.sort(list, comparator);",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableList.of(UTypeVar.create("E")),
            ImmutableMap.of(
                "collection", UClassType.create("java.util.Collection", UTypeVar.create("E")),
                "comparator",
                    UClassType.create(
                        "java.util.Comparator",
                        UWildcardType.create(BoundKind.SUPER, UTypeVar.create("E")))),
            UVariableDecl.create(
                "list",
                UTypeApply.create("java.util.List", UTypeVarIdent.create("E")),
                UNewClass.create(
                    UTypeApply.create("java.util.ArrayList", UTypeVarIdent.create("E")),
                    UFreeIdent.create("collection"))),
            UExpressionStatement.create(
                UMethodInvocation.create(
                    UStaticIdent.create(
                        "java.util.Collections",
                        "sort",
                        UForAll.create(
                            ImmutableList.of(UTypeVar.create("T")),
                            UMethodType.create(
                                UPrimitiveType.VOID,
                                UClassType.create("java.util.List", UTypeVar.create("T")),
                                UClassType.create(
                                    "java.util.Comparator",
                                    UWildcardType.create(BoundKind.SUPER, UTypeVar.create("T")))))),
                    ULocalVarIdent.create("list"),
                    UFreeIdent.create("comparator")))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void throwException() {
    compile(
        "class ThrowExceptionExample {",
        "  public void example() {",
        "    throw new IllegalArgumentException();",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            UThrow.create(
                UNewClass.create(UClassIdent.create("java.lang.IllegalArgumentException")))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void forEach() {
    compile(
        "class ForEachExample {",
        "  public void example(int[] array) {",
        "    int sum = 0;",
        "    for (int value : array) {",
        "      sum += value;",
        "    }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of("array", UArrayType.create(UPrimitiveType.INT)),
            UVariableDecl.create("sum", UPrimitiveTypeTree.INT, ULiteral.intLit(0)),
            UEnhancedForLoop.create(
                UVariableDecl.create("value", UPrimitiveTypeTree.INT),
                UFreeIdent.create("array"),
                UBlock.create(
                    UExpressionStatement.create(
                        UAssignOp.create(
                            ULocalVarIdent.create("sum"),
                            Kind.PLUS_ASSIGNMENT,
                            ULocalVarIdent.create("value")))))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void synchronizd() {
    compile(
        "class SynchronizedExample {",
        "  public void example(int[] array) {",
        "    int sum = 0;",
        "    synchronized (array) {",
        "      for (int value : array) {",
        "        sum += value;",
        "      }",
        "    }",
        "  }",
        "}");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of("array", UArrayType.create(UPrimitiveType.INT)),
            UVariableDecl.create("sum", UPrimitiveTypeTree.INT, ULiteral.intLit(0)),
            USynchronized.create(
                UParens.create(UFreeIdent.create("array")),
                UBlock.create(
                    UEnhancedForLoop.create(
                        UVariableDecl.create("value", UPrimitiveTypeTree.INT),
                        UFreeIdent.create("array"),
                        UBlock.create(
                            UExpressionStatement.create(
                                UAssignOp.create(
                                    ULocalVarIdent.create("sum"),
                                    Kind.PLUS_ASSIGNMENT,
                                    ULocalVarIdent.create("value")))))))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }

  @Test
  public void anonymousClass() {
    compile(
        "import java.util.Collections;",
        "import java.util.Comparator;",
        "import java.util.List;",
        "class AnonymousClassExample {",
        "  public void example(List<String> list) {",
        "    Collections.sort(list, new Comparator<String>() {",
        "      @Override public int compare(String a, String b) {",
        "        return a.compareTo(b);",
        "      }",
        "    });",
        "  }",
        "}");
    UTypeVar tVar = UTypeVar.create("T");
    assertEquals(
        BlockTemplate.create(
            ImmutableMap.of(
                "list", UClassType.create("java.util.List", UClassType.create("java.lang.String"))),
            UExpressionStatement.create(
                UMethodInvocation.create(
                    UStaticIdent.create(
                        "java.util.Collections",
                        "sort",
                        UForAll.create(
                            ImmutableList.of(tVar),
                            UMethodType.create(
                                UPrimitiveType.VOID,
                                UClassType.create("java.util.List", tVar),
                                UClassType.create(
                                    "java.util.Comparator",
                                    UWildcardType.create(BoundKind.SUPER, tVar))))),
                    UFreeIdent.create("list"),
                    UNewClass.create(
                        null,
                        ImmutableList.<UExpression>of(),
                        UTypeApply.create(
                            "java.util.Comparator", UClassIdent.create("java.lang.String")),
                        ImmutableList.<UExpression>of(),
                        UClassDecl.create(
                            UMethodDecl.create(
                                UModifiers.create(
                                    Flags.PUBLIC,
                                    UAnnotation.create(UClassIdent.create("java.lang.Override"))),
                                "compare",
                                UPrimitiveTypeTree.INT,
                                ImmutableList.of(
                                    UVariableDecl.create(
                                        "a", UClassIdent.create("java.lang.String")),
                                    UVariableDecl.create(
                                        "b", UClassIdent.create("java.lang.String"))),
                                ImmutableList.<UExpression>of(),
                                UBlock.create(
                                    UReturn.create(
                                        UMethodInvocation.create(
                                            UMemberSelect.create(
                                                ULocalVarIdent.create("a"),
                                                "compareTo",
                                                UMethodType.create(
                                                    UPrimitiveType.INT,
                                                    UClassType.create("java.lang.String"))),
                                            ULocalVarIdent.create("b")))))))))),
        UTemplater.createTemplate(context, getMethodDeclaration("example")));
  }
}
