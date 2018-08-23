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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author glorioso@google.com (Nick Glorioso) */
@RunWith(JUnit4.class)
public class QualifierOrScopeOnInjectMethodTest {

  private CompilationTestHelper compilationHelper;
  private BugCheckerRefactoringTestHelper refactoringHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(QualifierOrScopeOnInjectMethod.class, getClass());
    refactoringHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new QualifierOrScopeOnInjectMethod(), getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "class Foo {",
            "  // BUG: Diagnostic contains: @Inject  void someMethod() {}",
            "  @Inject @Named(\"bar\") void someMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_InjectConstructor() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "class Foo {",
            "  @Inject @Singleton @Named(\"bar\") Foo() {}",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.inject.Inject;",
            "import javax.inject.Named;",
            "import javax.inject.Singleton;",
            "@Singleton class Foo {",
            "  @Inject Foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeNotInject() {
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
}
