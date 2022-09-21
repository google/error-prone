/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.auto.value.processor.AutoBuilderProcessor;
import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.CheckReturnValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CheckReturnValueWellKnownLibrariesTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CheckReturnValue.class, getClass());

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // Don't match methods invoked through {@link org.mockito.Mockito}.
  @Test
  public void testIgnoreCRVOnMockito() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "public class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            " public int f() {",
            "    return 0;",
            "  }",
            "}")
        .addSourceLines(
            "TestCase.java",
            "import static org.mockito.Mockito.verify;",
            "import static org.mockito.Mockito.doReturn;",
            "import org.mockito.Mockito;",
            "class TestCase {",
            "  void m() {",
            "    lib.Test t = new lib.Test();",
            "    Mockito.verify(t).f();",
            "    verify(t).f();",
            "    doReturn(1).when(t).f();",
            "    Mockito.doReturn(1).when(t).f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTests() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Foo foo) {",
            "    try {",
            "      foo.f();",
            "      org.junit.Assert.fail();",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.Assert.fail();",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.TestCase.fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTestsWithRule() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private org.junit.rules.ExpectedException exception;",
            "  void f(Foo foo) {",
            "    exception.expect(IllegalArgumentException.class);",
            "    foo.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTestsWithFailureMessage() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Foo foo) {",
            "    try {",
            "      foo.f();",
            "      org.junit.Assert.fail(\"message\");",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.Assert.fail(\"message\");",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.TestCase.fail(\"message\");",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInThrowingRunnables() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Foo foo) {",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, ",
            "     new org.junit.function.ThrowingRunnable() {",
            "       @Override",
            "       public void run() throws Throwable {",
            "         foo.f();",
            "       }",
            "     });",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, () -> foo.f());",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, foo::f);",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, () -> {",
            "      int bah = foo.f();",
            "      foo.f(); ",
            "   });",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, () -> { ",
            "     // BUG: Diagnostic contains: CheckReturnValue",
            "     foo.f(); ",
            "     foo.f(); ",
            "   });",
            "   bar(() -> foo.f());",
            "   org.assertj.core.api.Assertions.assertThatExceptionOfType(IllegalStateException.class)",
            "      .isThrownBy(() -> foo.f());",
            "  }",
            "  void bar(org.junit.function.ThrowingRunnable r) {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreTruthFailure() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assert_;",
            "class Test {",
            "  void f(Foo foo) {",
            "    try {",
            "      foo.f();",
            "      assert_().fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void onlyIgnoreWithEnclosingTryCatch() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.fail;",
            "class Test {",
            "  void f(Foo foo) {",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    foo.f();",
            "    org.junit.Assert.fail();",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    foo.f();",
            "    junit.framework.Assert.fail();",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    foo.f();",
            "    junit.framework.TestCase.fail();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInOrderVerification() {
    compilationHelper
        .addSourceLines(
            "Lib.java",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  public int f() {",
            "    return 0;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.inOrder;",
            "class Test {",
            "  void m() {",
            "    inOrder().verify(new Lib()).f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usingElementInTestExpected() {
    compilationHelperLookingAtAllConstructors()
        .addSourceLines(
            "Foo.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.Test;",
            "@RunWith(JUnit4.class)",
            "class Foo {",
            "  @Test(expected = IllegalArgumentException.class) ",
            "  public void foo() {",
            "    new Foo();", // OK when it's the only statement
            "  }",
            "  @Test(expected = IllegalArgumentException.class) ",
            "  public void fooWith2Statements() {",
            "    Foo f = new Foo();",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    new Foo();", // Not OK if there is more than one statement in the block.
            "  }",
            "  @Test(expected = Test.None.class) ", // This is a weird way to spell the default
            "  public void fooWithNone() {",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    new Foo();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAutoValueBuilderSetterMethods() {
    compilationHelper
        .addSourceLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@AutoValue",
            "@CheckReturnValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  abstract int numberOfLegs();",
            "  static Builder builder() {",
            "    return new AutoValue_Animal.Builder();",
            "  }",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setName(String value);",
            "    abstract Builder setNumberOfLegs(int value);",
            "    abstract Animal build();",
            "  }",
            "}")
        .addSourceLines(
            "AnimalCaller.java",
            "package com.google.frobber;",
            "public final class AnimalCaller {",
            "  static void testAnimal() {",
            "    Animal.Builder builder = Animal.builder();",
            "    builder.setNumberOfLegs(4);", // AutoValue.Builder setters are implicitly @CIRV
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    builder.build();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testAutoValueBuilderSetterMethodsOnInterface() {
    compilationHelper
        .addSourceLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@AutoValue",
            "@CheckReturnValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  abstract int numberOfLegs();",
            "  static Builder builder() {",
            "    return new AutoValue_Animal.Builder();",
            "  }",
            "  @AutoValue.Builder",
            "  interface Builder {",
            "    Builder setName(String value);",
            "    Builder setNumberOfLegs(int numberOfLegs);",
            "    default Builder defaultMethod(int value) {",
            "      return new AutoValue_Animal.Builder();",
            "    }",
            "    Animal build();",
            "  }",
            "}")
        .addSourceLines(
            "AnimalCaller.java",
            "package com.google.frobber;",
            "public final class AnimalCaller {",
            "  static void testAnimal() {",
            "    Animal.Builder builder = Animal.builder();",
            "    builder.setName(\"Stumpy\");", // AutoValue.Builder setters are implicitly @CIRV
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    builder.defaultMethod(4);",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    builder.build();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testAutoValueGetterMethods() {
    compilationHelper
        .addSourceLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  abstract int numberOfLegs();",
            "}")
        .addSourceLines(
            "AnimalCaller.java",
            "package com.google.frobber;",
            "public final class AnimalCaller {",
            "  static void testAnimal() {",
            "    Animal a = new AutoValue_Animal(\"dog\", 4);",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    a.numberOfLegs();",
            "", // And test usages where the static type is the generated class, too:
            "    AutoValue_Animal b = new AutoValue_Animal(\"dog\", 4);",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    b.numberOfLegs();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testAutoBuilderSetterMethods() {
    compilationHelper
        .addSourceLines(
            "Person.java",
            "package com.google.frobber;",
            "public final class Person {",
            "  public Person(String name, int id) {}",
            "}")
        .addSourceLines(
            "PersonBuilder.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoBuilder;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "@AutoBuilder(ofClass = Person.class)",
            "interface PersonBuilder {",
            "  static PersonBuilder personBuilder() {",
            "    return new AutoBuilder_PersonBuilder();",
            "  }",
            "  PersonBuilder setName(String name);",
            "  PersonBuilder setId(int id);",
            "  Person build();",
            "}")
        .addSourceLines(
            "PersonCaller.java",
            "package com.google.frobber;",
            "public final class PersonCaller {",
            "  static void testPersonBuilder() {",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    PersonBuilder.personBuilder();",
            "    PersonBuilder builder = PersonBuilder.personBuilder();",
            "    builder.setName(\"kurt\");", // AutoBuilder setters are implicitly @CIRV
            "    builder.setId(42);", // AutoBuilder setters are implicitly @CIRV
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    builder.build();",
            "", // And test usages where the static type is the generated class, too:
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    new AutoBuilder_PersonBuilder().build();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoBuilderProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testAutoBuilderSetterMethods_withInterface() {
    compilationHelper
        .addSourceLines(
            "LogUtil.java",
            "package com.google.frobber;",
            "import java.util.logging.Level;",
            "public class LogUtil {",
            "  public static void log(Level severity, String message) {}",
            "}")
        .addSourceLines(
            "Caller.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoBuilder;",
            "import java.util.logging.Level;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "@AutoBuilder(callMethod = \"log\", ofClass = LogUtil.class)",
            "public interface Caller {",
            "  static Caller logCaller() {",
            "    return new AutoBuilder_Caller();",
            "  }",
            "  Caller setSeverity(Level level);",
            "  Caller setMessage(String message);",
            "  void call(); // calls: LogUtil.log(severity, message)",
            "}")
        .addSourceLines(
            "LogCaller.java",
            "package com.google.frobber;",
            "import java.util.logging.Level;",
            "public final class LogCaller {",
            "  static void testLogCaller() {",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    Caller.logCaller();",
            "    Caller caller = Caller.logCaller();",
            "    caller.setMessage(\"hi\");", // AutoBuilder setters are implicitly @CIRV
            "    caller.setSeverity(Level.FINE);", // AutoBuilder setters are implicitly @CIRV
            "    caller.call();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoBuilderProcessor.class.getName()))
        .doTest();
  }

  private CompilationTestHelper compilationHelperLookingAtAllConstructors() {
    return compilationHelper.setArgs(
        "-XepOpt:" + CheckReturnValue.CHECK_ALL_CONSTRUCTORS + "=true");
  }

  private CompilationTestHelper compilationHelperLookingAtAllMethods() {
    return compilationHelper.setArgs("-XepOpt:" + CheckReturnValue.CHECK_ALL_METHODS + "=true");
  }
}
