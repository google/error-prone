/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ReturnMissingNullable}Test */
@RunWith(JUnit4.class)
public class ReturnMissingNullableTest {

  @Test
  public void testLiteralNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return null;",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testParenthesizedLiteralNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return (null);",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignmentOfLiteralNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  String cachedMessage;",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return cachedMessage = null;",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCastLiteralNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return (String) null;",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testConditionalLiteralNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return x >= 0 ? null : \"negative\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testParenthesizedConditionalLiteralNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return (x >= 0 ? null : \"negative\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testVoidReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return getVoid();",
            "  }",
            "  abstract Void getVoid();",
            "}")
        .doTest();
  }

  @Test
  public void testSubtypeOfVoidReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get(Supplier<? extends Void> s) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return s.get();",
            "  }",
            "  interface Supplier<T> {",
            "    T get();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticFinalFieldAboveUsage() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  static final Object NULL = null;",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return NULL;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticFinalFieldBelowUsage() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return NULL;",
            "  }",
            "  static final Object NULL = null;",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceFinalField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  final Object nullObject = null;",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMemberSelectFinalField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  final Object nullObject = null;",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return this.nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMultipleFilesFinalField() {
    createCompilationTestHelper()
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  final Object nullObject = null;",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return nullObject;",
            "  }",
            "}")
        .addSourceLines(
            "Bar.java",
            "class Bar {",
            "  final Object nullObject = null;",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testVoidField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Void nullObject;",
            "  Object get() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testTypeAnnotatedArrayElement() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  @Nullable String[] getMessage(boolean b, String[] s) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return b ? s : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFinalLocalVariable() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get() {",
            "    final Object nullObject = null;",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testEffectivelyFinalLocalVariable() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get() {",
            "    Object nullObject = null;",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFinalLocalVariableComplexTree() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get(boolean b1, boolean b2, Object someObject) {",
            "    final Object nullObject = null;",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return (b1 ? someObject : b2 ? nullObject : \"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnXIfXIsNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return (o == null ? o : \"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnXUnlessXIsXNotNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return (o != null ? \"\" : o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnXInsideIfNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    if (o == null) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return o;",
            "    }",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnXInsideElseOfNotNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    if (o != null) {",
            "      return \"\";",
            "    } else {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return o;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnFieldInsideIfNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object o;",
            "  Object foo() {",
            "    if (o == null) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      return o;",
            "    }",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOtherVerify() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import static com.google.common.base.Verify.verify;",
            "class LiteralNullReturnTest {",
            "  public String getMessage(boolean b) {",
            "    verify(b);",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOrNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import com.google.common.base.Optional;",
            "class LiteralNullReturnTest {",
            "  public String getMessage(Optional<String> m) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return m.orNull();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOrElseNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import java.util.Optional;",
            "class LiteralNullReturnTest {",
            "  public String getMessage(Optional<String> m) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return m.orElse(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testImplementsMap() {
    createCompilationTestHelper()
        .addSourceLines(
            "NotMap.java", //
            "interface NotMap {",
            "  Integer get(String o);",
            "}")
        .addSourceLines(
            "MyMap.java",
            "import java.util.Map;",
            "interface MyMap<K, V> extends Map<K, V>, NotMap {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  @Override V get(Object o);",
            "  // BUG: Diagnostic contains: @Nullable",
            "  @Override V replace(K k, V v);",
            "  @Override boolean replace(K k, V expect, V update);",
            "  @Override Integer get(String o);",
            "}")
        .doTest();
  }

  @Test
  public void testOnlyIfAlreadyInScopeAndItIs() {
    createCompilationTestHelper()
        .setArgs("-XepOpt:Nullness:OnlyIfAnnotationAlreadyInScope=true")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  String getMessage(boolean b) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return b ? \"\" : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testArrayDeclaration() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class LiteralNullReturnTest {",
            "  public String[] getMessage(boolean b) {",
            "    return b ? null : new String[0];",
            "  }",
            "}")
        .addOutputLines(
            "out/com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class LiteralNullReturnTest {",
            "  @Nullable public String[] getMessage(boolean b) {",
            "    return b ? null : new String[0];",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testArrayTypeUse() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  public String[] getMessage(boolean b) {",
            "    return b ? null : new String[0];",
            "  }",
            "}")
        .addOutputLines(
            "out/com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  public String @Nullable [] getMessage(boolean b) {",
            "    return b ? null : new String[0];",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testArrayTypeUseTwoDimensional() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  public String[][] getMessage(boolean b, String[][] s) {",
            "    return b ? null : s;",
            "  }",
            "}")
        .addOutputLines(
            "out/com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  public String @Nullable [][] getMessage(boolean b, String[][] s) {",
            "    return b ? null : s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAlreadyTypeAnnotatedInnerClassMemberSelect() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  class Inner {}",
            "  LiteralNullReturnTest.@Nullable Inner getMessage(boolean b, Inner i) {",
            "    return b ? i : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAlreadyTypeAnnotatedInnerClassNonMemberSelect() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  class Inner {}",
            "  @Nullable Inner getMessage(boolean b, Inner i) {",
            "    return b ? i : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLimitation_staticFinalFieldInitializedLater() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  static final Object NULL;",
            "  static {",
            "    NULL = null;",
            "  }",
            "  Object get() {",
            "    return NULL;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLimitation_instanceFinalFieldInitializedLater() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  final Object nullObject;",
            "  {",
            "    nullObject = null;", // or, more likely, in a constructor
            "  }",
            "  Object get() {",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLimitation_finalLocalVariableInitializedLater() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object get() {",
            "    final Object nullObject;",
            "    nullObject = null;",
            "    return nullObject;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLimitation_returnThisXInsideIfNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object o;",
            "  Object foo() {",
            "    if (this.o == null) {",
            "      return this.o;",
            "    }",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testRemoveSuppressWarnings_removeNullnessReturnWarning() {
    createRefactoringTestHelper()
        .setArgs("-XepOpt:Nullness:RemoveSuppressWarnings=true")
        .addInputLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  @SuppressWarnings(\"nullness:return\")",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class LiteralNullReturnTest {",
            "  @Nullable ",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void testNegativeCases_onlyStatementIsNullReturn() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_typeVariableUsage() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public <T> T getMessage(boolean b, T t) {",
            "    return b ? null : t;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyAnnotated() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class LiteralNullReturnTest {",
            "  @Nullable",
            "  public String getMessage(boolean b) {",
            "    return b ? \"\" :null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyAnnotatedNullableDecl() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/NullableDecl.java",
            "package com.google.anno.my;",
            "public @interface NullableDecl {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import com.google.anno.my.NullableDecl;",
            "public class LiteralNullReturnTest {",
            "  @NullableDecl",
            "  public String getMessage(boolean b) {",
            "    return b ? \"\" :null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyAnnotatedNullableType() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.compatqual.NullableType;",
            "public class LiteralNullReturnTest {",
            "  public @NullableType String getMessage(boolean b) {",
            "    return b ? \"\" : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyTypeAnnotated() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "@Target({ElementType.TYPE_USE})",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/TypeAnnoReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class TypeAnnoReturnTest {",
            "  public @com.google.anno.my.Nullable String getMessage(boolean b) {",
            "    return b ? \"\" : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyDeclarationAnnotatedArray() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class LiteralNullReturnTest {",
            "  @Nullable",
            "  String[] getMessage(boolean b, String[] s) {",
            "    return b ? s : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyTypeAnnotatedArray() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  String @Nullable [] getMessage(boolean b, String[] s) {",
            "    return b ? s : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyTypeAnnotatedMemberSelect() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class LiteralNullReturnTest {",
            "  java.lang.@Nullable String getMessage(boolean b) {",
            "    return b ? \"\" : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_checkNotNullNullableInput() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullReturnTest {",
            "  @Nullable String message;",
            "  public String getMessage() {",
            "    return checkNotNull(message);",
            "  }",
            // One style of "check not null" method, whose type argument is unannotated, and accepts
            // a @Nullable input.
            "  private static <T> T checkNotNull(@Nullable T obj) {",
            "    if (obj==null) throw new NullPointerException();",
            "    return obj;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullArrayWithNullableElements() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "@Target({ElementType.TYPE_USE})",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import com.google.anno.my.Nullable;",
            "public class NullableParameterTest {",
            "  public String[] apply(@Nullable String[] message) {",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullLiteral() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    return \"hello\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullMethod() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullMethodTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullMethodTest {",
            "  public String getMessage(int x) {",
            "    return String.valueOf(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullFieldTest {",
            "  private String message;",
            "  public String getMessage() {",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullParameter() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullParameterTest {",
            "  public String apply(String message) {",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_this() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ThisTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class ThisTest {",
            "  private String message;",
            "  public ThisTest setMessage(String message) {",
            "    this.message = message;",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_capturedLocal() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/CapturedLocalTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public abstract class CapturedLocalTest {",
            "  public abstract String getMessage();",
            "  public CapturedLocalTest withMessage(final String message) {",
            "    return new CapturedLocalTest() {",
            "      public String getMessage() {",
            "        return message;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Makes sure the check never flags methods returning a primitive. Returning null from them is a
   * bug, of course, but we're not trying to find those bugs in this check.
   */
  @Test
  public void testNegativeCases_primitiveReturnType() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/PrimitiveReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class PrimitiveReturnTest {",
            "  public int getCount() {",
            "    return (Integer) null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_voidMethod() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/VoidMethodTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class VoidMethodTest {",
            "  public void run(int iterations) {",
            "    if (iterations <= 0) { return; }",
            "    run(iterations - 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_voidTypedMethod() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/VoidTypeTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class VoidTypeTest {",
            "  public Void run(int iterations) {",
            "    if (iterations <= 0) { return null; }",
            "    run(iterations - 1);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nullableReturnInLambda() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class MissingNullableReturnTest {",
            "  public static final java.util.function.Function<String, String> IDENTITY =",
            "      (s -> { return s != null ? s : null; });",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnLambda() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class MissingNullableReturnTest {",
            "  public static java.util.function.Function<String, String> identity() {",
            "    return s -> s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnParenthesizedLambda() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class MissingNullableReturnTest {",
            "  public static java.util.function.Function<String, String> identity() {",
            "    return (s -> s);",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for b/110812469; verifies that untracked access paths that mix field access
  // and method invocation are "trusted" to yield nonNull values
  @Test
  public void testNegativeCases_mixedMethodFieldAccessPath() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nonnull;",
            "public class MissingNullableReturnTest {",
            "  public @Nonnull MyClass test() {",
            "    return ((MyClass) null).myMethod().myField;",
            "  }",
            "  abstract class MyClass {",
            "    abstract MyClass myMethod();",
            "    MyClass myField;",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for b/113123074
  @Test
  public void testNegativeCases_delegate() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "import java.util.Optional;",
            "public class MissingNullableReturnTest {",
            "  public String get() {",
            "    return getInternal(true, null);",
            "  }",
            "  private String getInternal(boolean flag, @Nullable Integer i) {",
            "    return \"hello\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_lambda() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "import java.util.concurrent.Callable;",
            "public class MissingNullableReturnTest {",
            "  public Callable<?> get() {",
            "    return () -> { return null; };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_staticNonFinalField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  static Object NULL = null;",
            "  Object get() {",
            "    return NULL;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_polyNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.PolyNull;",
            "public class LiteralNullReturnTest {",
            "  public @PolyNull String getMessage(@PolyNull String s) {",
            "    if (s == null) {",
            "      return null;",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_unreachableExit() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    System.exit(1);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_unreachableFail() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import static org.junit.Assert.fail;",
            "class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    fail();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_unreachableThrowExceptionMethod() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import static org.junit.Assert.fail;",
            "class LiteralNullReturnTest {",
            "  void throwRuntimeException() {}",
            "  public String getMessage() {",
            "    throwRuntimeException();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_unreachableCheckFalse() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import static com.google.common.base.Preconditions.checkState;",
            "class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    checkState(false);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_unreachableVerifyFalse() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import static com.google.common.base.Verify.verify;",
            "class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    verify(false);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_staticFinalNonNullField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  static final Object SOMETHING = 1;",
            "  Object get() {",
            "    return SOMETHING;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnXIfXIsNotNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    return (o != null ? o : \"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnXIfSameSymbolDifferentObjectIsNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object o;",
            "  Object foo(LiteralNullReturnTest other) {",
            "    return (o == null ? other.o : \"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnXUnlessXIsXNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    return (o == null ? \"\" : o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnXInsideIfNotNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    if (o != null) {",
            "      return o;",
            "    }",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnXInsideIfNullElse() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    if (o == null) {",
            "      return \"\";",
            "    } else {",
            "      return o;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnXInsideIfNullButAfterOtherStatement() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object foo(Object o) {",
            "    if (o == null) {",
            "      o = \"\";",
            "      return o;",
            "    }",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_onlyIfAlreadyInScopeAndItIsNot() {
    createCompilationTestHelper()
        .setArgs("-XepOpt:Nullness:OnlyIfAnnotationAlreadyInScope=true")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  String getMessage(boolean b) {",
            "    return b ? \"\" : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_orElseNotNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import java.util.Optional;",
            "class LiteralNullReturnTest {",
            "  public String getMessage(Optional<String> m) {",
            "    return m.orElse(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_doesNotImplementMap() {
    createCompilationTestHelper()
        .addSourceLines(
            "NotMap.java",
            "interface NotMap<K, V> {",
            "  String get(Object o);",
            "  V replace(K k, V v);",
            "}")
        .addSourceLines(
            "MyMap.java",
            "interface MyMap extends NotMap<Integer, Double> {",
            "  @Override String get(Object o);",
            "  @Override Double replace(Integer k, Double v);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_suppressionForReturnTreeBased() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import java.util.Optional;",
            "class LiteralNullReturnTest {",
            "  @SuppressWarnings(\"ReturnMissingNullable\")",
            "  public String getMessage(Optional<String> m) {",
            "    return m.orElse(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_suppressionForMethodTreeBased() {
    createCompilationTestHelper()
        .addSourceLines(
            "NotMap.java", //
            "interface NotMap {",
            "  Integer get(String o);",
            "}")
        .addSourceLines(
            "MyMap.java",
            "import java.util.Map;",
            "interface MyMap<K, V> extends Map<K, V>, NotMap {",
            "  @SuppressWarnings(\"ReturnMissingNullable\")",
            "  @Override V get(Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_suppressionAboveMethodLevel() {
    createCompilationTestHelper()
        .addSourceLines(
            "NotMap.java", //
            "interface NotMap {",
            "  Integer get(String o);",
            "}")
        .addSourceLines(
            "MyMap.java",
            "import java.util.Map;",
            "@SuppressWarnings(\"ReturnMissingNullable\")",
            "interface MyMap<K, V> extends Map<K, V>, NotMap {",
            "  @Override V get(Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testReturnSameSymbolDifferentObjectInsideIfNull() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "abstract class LiteralNullReturnTest {",
            "  Object o;",
            "  Object foo(LiteralNullReturnTest other) {",
            "    if (o == null) {",
            "      return other.o;",
            "    }",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestNonJsr305Nullable() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  @Nullable private final Object obj1 = null;",
            "  private final Object method(boolean b) { return b ? null : 0; }",
            "  @interface Nullable {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class T {",
            "  @Nullable private final Object obj1 = null;",
            "  @Nullable private final Object method(boolean b) { return b ? null : 0; }",
            "  @interface Nullable {}",
            "}")
        .doTest();
  }

  @Test
  public void testNonAnnotationNullable() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  private final Object method(boolean b) { return b ? null : 0; }",
            "  class Nullable {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class T {",
            "  @javax.annotation.Nullable private final Object method(boolean b) { return b ? null"
                + " : 0; }",
            "  class Nullable {}",
            "}")
        .doTest();
  }

  @Test
  public void testMultipleNullReturns() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  private final Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import javax.annotation.Nullable;",
            "class T {",
            "  @Nullable private final Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMemberSelectReturnType() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  java.lang.Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  java.lang.@Nullable Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotationInsertedAfterModifiers() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  final Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  final @Nullable Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void testParameterizedMemberSelectReturnType() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  java.util.List<java.lang.Object> method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  java.util.@Nullable List<java.lang.Object> method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedMemberSelectReturnType() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "import org.checkerframework.checker.initialization.qual.UnderInitialization;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  java.lang.@UnderInitialization Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import org.checkerframework.checker.initialization.qual.UnderInitialization;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  java.lang.@Nullable @UnderInitialization Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotationNotNamedNullable() {
    createRefactoringTestHelper()
        .setArgs("-XepOpt:Nullness:DefaultNullnessAnnotation=javax.annotation.CheckForNull")
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import javax.annotation.CheckForNull;",
            "class T {",
            "  @CheckForNull Object method(boolean b) {",
            "    if (b) {",
            "      return null;",
            "    } else {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAggressive_onlyStatementIsNullReturn() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAggressive_typeVariableUsage() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public <T> T getMessage(boolean b, T t) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return b ? null : t;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAggressive_voidTypedMethod() {
    createAggressiveCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/VoidTypeTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class VoidTypeTest {",
            "  public Void run(int iterations) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    if (iterations <= 0) { return null; }",
            "    run(iterations - 1);",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_doesNotRemoveNecessarySuppressWarnings() {
    createRefactoringTestHelper()
        .setArgs("-XepOpt:Nullness:RemoveSuppressWarnings=true")
        .addInputLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  @SuppressWarnings(\"nullness:argument\")",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      doSomethingElse(null);",
            "     return \"negative\";",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "  public void doSomethingElse(Object c) {",
            "      return;",
            "  }",
            "}")
        .addOutputLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  @SuppressWarnings(\"nullness:argument\")",
            "  public String getMessage(boolean b) {",
            "    if (b) {",
            "      doSomethingElse(null);",
            "     return \"negative\";",
            "    } else {",
            "      return \"negative\";",
            "    }",
            "  }",
            "  public void doSomethingElse(Object c) {",
            "      return;",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(ReturnMissingNullable.class, getClass());
  }

  private CompilationTestHelper createAggressiveCompilationTestHelper() {
    return createCompilationTestHelper().setArgs("-XepOpt:Nullness:Conservative=false");
  }

  private BugCheckerRefactoringTestHelper createRefactoringTestHelper() {
    return BugCheckerRefactoringTestHelper.newInstance(ReturnMissingNullable.class, getClass());
  }
}
