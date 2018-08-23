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
package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author Gregory Kick (gak@google.com) */
@RunWith(JUnit4.class)
public final class PrivateConstructorForNoninstantiableModuleTest {
  private BugCheckerRefactoringTestHelper testHelper;

  @Before
  public void setUp() {
    testHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new PrivateConstructorForNoninstantiableModule(), getClass());
  }

  @Test
  public void emptyModuleGetsLeftAlone() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import dagger.Module;",
            "@Module class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void onlyStaticMethods() {
    testHelper
        .addInputLines(
            "in/TestModule.java", //
            "import dagger.Module;",
            "import dagger.Provides;",
            "@Module final class TestModule {",
            "  @Provides static String provideString() { return \"\"; }",
            "  @Provides static Integer provideInteger() { return 1; }",
            "}")
        .addOutputLines(
            "out/TestModule.java", //
            "import dagger.Module;",
            "import dagger.Provides;",
            "@Module final class TestModule {",
            "  @Provides static String provideString() { return \"\"; }",
            "  @Provides static Integer provideInteger() { return 1; }",
            "  private TestModule() {}",
            "}")
        .doTest();
  }

  @Test
  public void onlyStaticMethods_withConstructorGetsLeftAlone() {
    testHelper
        .addInputLines(
            "in/TestModule.java", //
            "import dagger.Module;",
            "import dagger.Provides;",
            "@Module final class TestModule {",
            "  @Provides static String provideString() { return \"\"; }",
            "  @Provides static Integer provideInteger() { return 1; }",
            "  private TestModule() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void abstractClassWithStaticAndAbstractMethods() {
    testHelper
        .addInputLines(
            "in/TestModule.java",
            "import dagger.Binds;",
            "import dagger.Module;",
            "import dagger.Provides;",
            "@Module abstract class TestModule {",
            "  @Provides static String provideString() { return \"\"; }",
            "  @Binds abstract Object bindObject(String string);",
            "  @Provides static Integer provideInteger() { return 1; }",
            "  @Binds abstract Number bindNumber(Integer integer);",
            "}")
        .addOutputLines(
            "out/TestModule.java", //
            "import dagger.Binds;",
            "import dagger.Module;",
            "import dagger.Provides;",
            "@Module abstract class TestModule {",
            "  @Provides static String provideString() { return \"\"; }",
            "  @Binds abstract Object bindObject(String string);",
            "  @Provides static Integer provideInteger() { return 1; }",
            "  @Binds abstract Number bindNumber(Integer integer);",
            "  private TestModule() {}",
            "}")
        .doTest();
  }

  @Test
  public void abstractClassWithStaticAndAbstractMethods_withConstructorGetsLeftAlone() {
    testHelper
        .addInputLines(
            "in/TestModule.java",
            "import dagger.Binds;",
            "import dagger.Module;",
            "import dagger.Provides;",
            "@Module abstract class TestModule {",
            "  @Provides static String provideString() { return \"\"; }",
            "  @Binds abstract Object bindObject(String string);",
            "  @Provides static Integer provideInteger() { return 1; }",
            "  @Binds abstract Number bindNumber(Integer integer);",
            "  private TestModule() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void onlyAbstractMethods() {
    testHelper
        .addInputLines(
            "in/TestModule.java",
            "import dagger.Binds;",
            "import dagger.Module;",
            "@Module abstract class TestModule {",
            "  @Binds abstract Object bindObject(String string);",
            "  @Binds abstract Number bindNumber(Integer integer);",
            "}")
        .addOutputLines(
            "out/TestModule.java", //
            "import dagger.Binds;",
            "import dagger.Module;",
            "@Module abstract class TestModule {",
            "  @Binds abstract Object bindObject(String string);",
            "  @Binds abstract Number bindNumber(Integer integer);",
            "  private TestModule() {}",
            "}")
        .doTest();
  }

  @Test
  public void onlyAbstractMethods_withConstructorGetsLeftAlone() {
    testHelper
        .addInputLines(
            "in/TestModule.java",
            "import dagger.Binds;",
            "import dagger.Module;",
            "@Module abstract class TestModule {",
            "  @Binds abstract Object bindObject(String string);",
            "  @Binds abstract Number bindNumber(Integer integer);",
            "  private TestModule() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void interfaceModuleGetsLeftAlone() {
    testHelper
        .addInputLines(
            "in/TestModule.java",
            "import dagger.Binds;",
            "import dagger.Module;",
            "@Module interface TestModule {",
            "  @Binds Object bindObject(String string);",
            "  @Binds Number bindNumber(Integer integer);",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
