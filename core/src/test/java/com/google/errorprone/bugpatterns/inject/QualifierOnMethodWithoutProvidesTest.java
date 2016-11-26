/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author glorioso@google.com (Nick Glorioso) */
@RunWith(JUnit4.class)
public class QualifierOnMethodWithoutProvidesTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(QualifierOnMethodWithoutProvides.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Named;",
            "class Foo {",
            "  // BUG: Diagnostic contains: void someMethod() {}",
            "  @Named(\"bar\") void someMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_insideGuiceModule() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Named;",
            "import com.google.inject.AbstractModule;",
            "class Foo extends AbstractModule {",
            "  protected void configure() {}",
            "  // BUG: Diagnostic contains: @Provides @Named(\"bar\") int someMethod",
            "  @Named(\"bar\") int someMethod() { return 42; }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_insideGuiceModule_butVoidReturning() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Named;",
            "import com.google.inject.AbstractModule;",
            "class Foo extends AbstractModule {",
            "  protected void configure() {}",
            "  // BUG: Diagnostic contains: remove",
            "  @Named(\"bar\")",
            "  void someMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_insideGuiceModule_butBoxedVoidReturning() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Named;",
            "import com.google.inject.AbstractModule;",
            "class Foo extends AbstractModule {",
            "  protected void configure() {}",
            "  // BUG: Diagnostic contains: remove",
            "  @Named(\"bar\")",
            "  Void someMethod() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_insideDaggerComponent() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Named;",
            "import dagger.Module;",
            "@Module class Foo {",
            "  // BUG: Diagnostic contains: @Provides @Named(\"bar\") int someMethod()",
            "  @Named(\"bar\") int someMethod() { return 42; }",
            "}")
        .doTest();
  }

  @Test
  public void testNegative() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import dagger.Provides;",
            "import dagger.Module;",
            "import javax.inject.Named;",
            "@Module",
            "class Foo {",
            "  @Provides @Named(\"bar\") int something() { return 42; }",
            "}")
        .doTest();
  }

  @Test
  public void testNegative_ginModule() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Named;",
            "import com.google.gwt.inject.client.Ginjector;",
            "interface Foo extends Ginjector {",
            "  @Named(\"bar\") int something();",
            "}")
        .doTest();
  }
}
