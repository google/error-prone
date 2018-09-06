/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.annotations.Var;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class FieldCanBeFinalTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FieldCanBeFinal.class, getClass());

  @Test
  public void annotationFieldsAreAlreadyFinal() {
    compilationHelper
        .addSourceLines(
            "Anno.java", //
            "public @interface Anno {",
            "  int x = 42;",
            "  static int y = 42;",
            "}")
        .doTest();
  }

  @Test
  public void simple() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private final int x",
            "  private int x;",
            "  Test() {",
            "    x = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void initializerBlocks() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private final int x1",
            "  private int x1;",
            "  private int x2;",
            "  // BUG: Diagnostic contains: private static final int y1",
            "  private static int y1;",
            "  private static int y2;",
            "  {",
            "    x1 = 42;",
            "    x2 = 42;",
            "  }",
            "  static {",
            "    y1 = 42;",
            "    y2 = 42;",
            "  }",
            "  void mutate() {",
            "    x2 = 0;",
            "    y2 = 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticSetFromInstance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private static final int x1",
            "  private static int x1;",
            "  private static int x2;",
            "  static {",
            "    x1 = 42;",
            "    x2 = 42;",
            "  }",
            "  {",
            "    x2 = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressionOnField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"FieldCanBeFinal\")",
            "  private int x;",
            "  Test() {",
            "    x = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressionOnClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@SuppressWarnings(\"FieldCanBeFinal\") ",
            "class Test {",
            "  private int x;",
            "  Test() {",
            "    x = 42;",
            "  }",
            "}")
        .doTest();
  }

  // the nullary constructor doesn't set x directly, but that's OK
  @Test
  public void constructorChaining() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private final int x",
            "  private int x;",
            "  Test(int x) {",
            "    this.x = x;",
            "  }",
            "  Test() {",
            "    this(42);",
            "  }",
            "}")
        .doTest();
  }

  // we currently handle this by ignoring control flow and looking for at least one initialization
  @Test
  public void controlFlow() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private final int x",
            "  private int x;",
            "  Test(boolean flag, int x, int y) {",
            "    if (flag) {",
            "      this.x = x;",
            "    } else {",
            "      this.x = y;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doubleInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int x;",
            "  Test(int x) {",
            "    this.x = x;",
            "    this.x = x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void compoundAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int x;",
            "  Test() {",
            "    this.x = 42;",
            "  }",
            "  void incr() {",
            "    x += 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unaryAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int x;",
            "  Test() {",
            "    this.x = 42;",
            "  }",
            "  void incr() {",
            "    x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructorAssignmentToOtherInstance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int x;",
            "  // BUG: Diagnostic contains: private final int y",
            "  private int y;",
            "  Test(Test other) {",
            "    x = 42;",
            "    y = 42;",
            "    other.x = x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentFromOutsideCompilationUnit() {
    compilationHelper
        .addSourceLines(
            "A.java",
            "class A {",
            "  int x;",
            "  A(B b) {",
            "    x = 42;",
            "    b.x = 42;",
            "  }",
            "}")
        .addSourceLines(
            "B.java",
            "class B {",
            "  int x;",
            "  B(A a) {",
            "    x = 42;",
            "    a.x = 42;",
            "  }",
            "}")
        // hackily force processing of both compilation units so we can verify both diagnostics
        .setArgs(Arrays.asList("-XDshouldStopPolicyIfError=FLOW"))
        .doTest();
  }

  // don't report an error if the field has an initializer and is also written
  // in the constructor
  @Test
  public void guardInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            String.format("import %s;", Var.class.getCanonicalName()),
            "class Test {",
            "  private boolean initialized = false;",
            "  Test() {",
            "    initialized = true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private final boolean flag",
            "  private boolean flag = false;",
            "  Test() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInject() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.inject.Inject;",
            "class Test {",
            "  @Inject private Object x;",
            "  Test() {",
            "    this.x = x;",
            "  }",
            "}")
        .doTest();
  }


  @Test
  public void allowNonFinal_nonFinalForTesting() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@interface NonFinalForTesting {}",
            "class Test {",
            "  @NonFinalForTesting private int x;",
            "  Test(int x) {",
            "    this.x = x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void visibleForTesting() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.annotations.VisibleForTesting;",
            "class Test {",
            "  @VisibleForTesting public int x;",
            "  Test() {",
            "    x = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void protectedField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.annotations.VisibleForTesting;",
            "class Test {",
            "  protected int x;",
            "  Test() {",
            "    x = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonPrivateField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.annotations.VisibleForTesting;",
            "class Test {",
            "  public int x;",
            "  int y;",
            "  Test() {",
            "    x = 42;",
            "    y = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowObjectifyClasses() {
    compilationHelper
        .addSourceLines(
            "com/googlecode/objectify/v4/annotation/Entity.java",
            "package com.googlecode.objectify.v4.annotation;",
            "public @interface Entity {}")
        .addSourceLines(
            "Test.java",
            "import com.googlecode.objectify.v4.annotation.Entity;",
            "@Entity class Test {",
            "  private int x;",
            "  Test(int x) {",
            "    this.x = x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void initInLambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int x;",
            "  private final Runnable r;",
            "  Test() {",
            "    r = () -> x = 1;",
            "  }",
            "}")
        .doTest();
  }
}
