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

package com.google.errorprone.bugpatterns.inject;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link MisplacedScopeAnnotations}. */
@RunWith(JUnit4.class)
public class MisplacedScopeAnnotationsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MisplacedScopeAnnotations.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new MisplacedScopeAnnotations(), getClass());

  @Test
  public void testPositiveCase_methodInjection() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  // BUG: Diagnostic contains: @Inject void someMethod( String foo) {}",
            "  @Inject void someMethod(@Singleton String foo) {}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_providerMethod() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import com.google.inject.Provides;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Provides String provideString(@Singleton @Named(\"foo\") String foo) {",
            "    return foo;",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import com.google.inject.Provides;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Provides String provideString( @Named(\"foo\") String foo) {",
            "    return foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_injectConstructor() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject Foo(@Singleton @Named(\"bar\") String bar) {}",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject Foo( @Named(\"bar\") String bar) {}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_injectConstructorMultipleAnnotations() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject Foo(@Singleton String bar, Integer i, @Singleton Long c) {}",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject Foo( String bar, Integer i,  Long c) {}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_fieldInjection() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject @Singleton String foo;",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject  String foo;",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase_noScopeAnnotationOnInjectedParameters() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import dagger.Provides;",
            "import dagger.Module;",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "@Module",
            "class Foo {",
            "  @Provides @Singleton @Named(\"bar\")",
            "  int something(@Named(\"bar\") Integer bar) {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase_scopeAnnotationIsAlsoQualifier() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import dagger.Provides;",
            "import dagger.Module;",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Qualifier;",
            "import javax.inject.Scope;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "@Module",
            "class Foo {",
            "  @Qualifier",
            "  @Scope",
            "  @Retention(RetentionPolicy.RUNTIME)",
            "  @interface RequestScoped {}",
            "                             ",
            "  @Provides",
            "  int something(@RequestScoped Integer bar) {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }
}
