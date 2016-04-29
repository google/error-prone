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

package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UseBinds}.
 */
@RunWith(JUnit4.class)
public class UseBindsTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(UseBinds.class, getClass());
  }

  @Test
  public void staticProvidesMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@Module",
            "class Test {",
            "    // BUG: Diagnostic contains: @Binds is a more efficient and declaritive mechanism "
                + "for delegating a binding",
            "  @Provides static Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instanceProvidesMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@Module",
            "class Test {",
            "    // BUG: Diagnostic contains: @Binds is a more efficient and declaritive mechanism "
                + "for delegating a binding",
            "  @Provides Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleBindsMethods() {
    compilationHelper
    .addSourceLines(
        "Test.java",
        "import dagger.Module;",
        "import dagger.Provides;",
        "import java.security.SecureRandom;",
        "import java.util.Random;",
        "@Module",
        "class Test {",
        "    // BUG: Diagnostic contains: @Binds is a more efficient and declaritive mechanism "
                + "for delegating a binding",
            "  @Provides Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "    // BUG: Diagnostic contains: @Binds is a more efficient and declaritive mechanism "
                + "for delegating a binding",
            "  @Provides Object provideRandomObject(SecureRandom impl) {",
            "    return impl;",
            "  }",
        "}")
    .doTest();
  }

  @Test
  public void instanceProvidesMethodWithInstanceSibling() {
    compilationHelper
    .addSourceLines(
        "Test.java",
        "import dagger.Module;",
        "import dagger.Provides;",
        "import java.security.SecureRandom;",
        "import java.util.Random;",
        "@Module",
        "class Test {",
        "  @Provides Random provideRandom(SecureRandom impl) {",
        "    return impl;",
        "  }",

        "  @Provides SecureRandom provideSecureRandom() {",
        "    return new SecureRandom();",
        "  }",
        "}")
    .doTest();
  }

  @Test
  public void instanceProvidesMethodWithStaticSibling() {
    compilationHelper
    .addSourceLines(
        "Test.java",
        "import dagger.Module;",
        "import dagger.Provides;",
        "import java.security.SecureRandom;",
        "import java.util.Random;",
        "@Module",
        "class Test {",
        "    // BUG: Diagnostic contains: @Binds is a more efficient and declaritive mechanism "
            + "for delegating a binding",
        "  @Provides Random provideRandom(SecureRandom impl) {",
        "    return impl;",
        "  }",

        "  @Provides static SecureRandom provideRandom() {",
        "    return new SecureRandom();",
        "  }",
        "}")
    .doTest();
  }

  @Test
  public void notABindMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import java.util.Random;",
            "@Module",
            "class Test {",
            "  @Provides Random provideRandom() {",
            "    return new Random();",
            "  }",
            "}")
        .doTest();
  }

}
