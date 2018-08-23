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
import dagger.Module;
import dagger.Provides;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link UseBinds}. */
@RunWith(Parameterized.class)
public class UseBindsTest {
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {Provides.class.getCanonicalName(), Module.class.getCanonicalName()},
          {Produces.class.getCanonicalName(), ProducerModule.class.getCanonicalName()}
        });
  }

  private final String bindingMethodAnnotation;
  private final String moduleAnnotation;

  private BugCheckerRefactoringTestHelper testHelper;

  public UseBindsTest(String bindingMethodAnnotation, String moduleAnnotation) {
    this.bindingMethodAnnotation = bindingMethodAnnotation;
    this.moduleAnnotation = moduleAnnotation;
  }

  @Before
  public void setUp() {
    testHelper = BugCheckerRefactoringTestHelper.newInstance(new UseBinds(), getClass());
  }

  @Test
  public void staticProvidesMethod() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  static Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Binds;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Binds abstract Random provideRandom(SecureRandom impl);",
            "}")
        .doTest();
  }

  @Test
  public void intoSetMethod() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import dagger.multibindings.IntoSet;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  @IntoSet static Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Binds;",
            "import dagger.multibindings.IntoSet;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Binds @IntoSet abstract Random provideRandom(SecureRandom impl);",
            "}")
        .doTest();
  }

  @Test
  public void instanceProvidesMethod() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Binds;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Binds abstract Random provideRandom(SecureRandom impl);",
            "}")
        .doTest();
  }

  @Test
  public void multipleBindsMethods() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "  @" + bindingMethodAnnotation,
            "  Object provideRandomObject(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Binds;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Binds abstract Random provideRandom(SecureRandom impl);",
            "  @Binds abstract Object provideRandomObject(SecureRandom impl);",
            "}")
        .doTest();
  }

  @Test
  public void instanceProvidesMethodWithInstanceSibling() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "  @" + bindingMethodAnnotation,
            "  SecureRandom provideSecureRandom() {",
            "    return new SecureRandom();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void instanceProvidesMethodWithStaticSibling() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  Random provideRandom(SecureRandom impl) {",
            "    return impl;",
            "  }",
            "  @" + bindingMethodAnnotation,
            "  static SecureRandom provideRandom() {",
            "    return new SecureRandom();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Binds;",
            "import java.security.SecureRandom;",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "abstract class Test {",
            "  @Binds abstract Random provideRandom(SecureRandom impl);",
            "  @" + bindingMethodAnnotation,
            "  static SecureRandom provideRandom() {",
            "    return new SecureRandom();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notABindsMethod() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Random;",
            "@" + moduleAnnotation,
            "class Test {",
            "  @" + bindingMethodAnnotation,
            "  Random provideRandom() {",
            "    return new Random();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

}
