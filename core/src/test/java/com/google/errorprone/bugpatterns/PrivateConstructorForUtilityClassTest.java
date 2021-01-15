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
package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link PrivateConstructorForUtilityClass} Test */
@RunWith(JUnit4.class)
public final class PrivateConstructorForUtilityClassTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          new PrivateConstructorForUtilityClass(), getClass());

  @Test
  public void emptyClassesGetLeftAlone() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void privateClassesGetLeftAlone() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  private static class Blah {",
            "    static void blah() {}",
            "  }",
            "  private Test() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void subClassesGetLeftAlone() {
    testHelper
        .addInputLines(
            "in/Foo.java", //
            "public class Foo<E> {",
            "  private E entity;",
            "  public E getEntity() {",
            "    return entity;",
            "  }",
            "  public void setEntity(E anEntity) {",
            "    entity = anEntity;",
            "  }",
            "  public static class BooleanFoo extends Foo<Boolean> {",
            "    private static final long serialVersionUID = 123456789012L;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void implementingClassesGetLeftAlone() {
    testHelper
        .addInputLines(
            "in/Foo.java", //
            "import java.io.Serializable;",
            "public class Foo {",
            "  private int entity;",
            "  public int getEntity() {",
            "    return entity;",
            "  }",
            "  public void setEntity(int anEntity) {",
            "    entity = anEntity;",
            "  }",
            "  public static class Bar implements Serializable {",
            "    private static final long serialVersionUID = 123456789012L;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void privateScopedClassesGetLeftAlone() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  private static class Blah {",
            "    static class Bleh {",
            "      static void bleh() {}",
            "    }",
            "  }",
            "  private Test() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void utilityClassesGetAPrivateConstructor_onlyFields() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "",
            "final class Test {",
            "  static final String SOME_CONSTANT = \"\";",
            "}")
        .addOutputLines(
            "out/Test.java",
            "",
            "final class Test {",
            "  static final String SOME_CONSTANT = \"\";",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void utilityClassesGetAPrivateConstructor_onlyMethods() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  static void blah() {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "",
            "final class Test {",
            "  static void blah() {}",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void utilityClassesGetAPrivateConstructor_onlyNestedClasses() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  static class Blah {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "",
            "final class Test {",
            "  static class Blah {}",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void utilityClassesGetAPrivateConstructor_onlyStaticInitializer() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  static {}",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "final class Test {",
            "  static {}",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void utilityClassesWithAConstructorGetLeftAlone() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  static void blah() {}",
            "  Test() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_field() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  private Object blah;",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_method() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  void blah() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_innerClass() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  class Blah {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_initializer() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_constructor() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "final class Test {",
            "  Test() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_interface() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "interface Test {",
            "  void blah();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void otherClassesGetLeftAlone_enum() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "enum Test {",
            "  INSTANCE;",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void b30170662() {
    CompilationTestHelper.newInstance(PrivateConstructorForUtilityClass.class, getClass())
        .addSourceLines(
            "Foo.java",
            "// BUG: Diagnostic contains:",
            "public class Foo {",
            "  enum Enum {}",
            "  @interface Annotation {}",
            "  interface Interface {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreTestClasses() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "final class Test {",
            "  @RunWith(JUnit4.class)",
            "  private static class Blah {",
            "    static void blah() {}",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void finalAdded() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "",
            "class Test {",
            "  static final String SOME_CONSTANT = \"\";",
            "}")
        .addOutputLines(
            "out/Test.java",
            "",
            "final class Test {",
            "  static final String SOME_CONSTANT = \"\";",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void abstractClass_noPrivateConstructor() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "",
            "abstract class Test {",
            "  static final String SOME_CONSTANT = \"\";",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
