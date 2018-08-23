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

package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ScopeOnModule}. */
@RunWith(JUnit4.class)
public class ScopeOnModuleTest {
  private BugCheckerRefactoringTestHelper testHelper;

  @Before
  public void setUp() {
    testHelper = BugCheckerRefactoringTestHelper.newInstance(new ScopeOnModule(), getClass());
  }

  @Test
  public void removeScope() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import javax.inject.Singleton;",
            "",
            "@Module",
            "@Singleton",
            "class Test {",
            "  @Provides",
            "  @Singleton",
            "  Object provideObject() {",
            "    return new Object();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import javax.inject.Singleton;",
            "",
            "@Module",
            "class Test {",
            "  @Provides",
            "  @Singleton",
            "  Object provideObject() {",
            "    return new Object();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void customScope() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import javax.inject.Scope;",
            "",
            "@Module",
            "@Test.MyScope",
            "class Test {",
            "  @Scope @interface MyScope {}",
            "",
            "  @Provides",
            "  @MyScope",
            "  Object provideObject() {",
            "    return new Object();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import javax.inject.Scope;",
            "",
            "@Module",
            "class Test {",
            "  @Scope @interface MyScope {}",
            "",
            "  @Provides",
            "  @MyScope",
            "  Object provideObject() {",
            "    return new Object();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notAScope() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "",
            "@Module",
            "@Test.NotAScope",
            "class Test {",
            "  @interface NotAScope {}",
            "",
            "  @Provides",
            "  Object provideObject() {",
            "    return new Object();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
