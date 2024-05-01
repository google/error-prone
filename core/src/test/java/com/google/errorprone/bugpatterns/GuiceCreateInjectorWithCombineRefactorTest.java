/*
 * Copyright 2024 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GuiceCreateInjectorWithCombineRefactorTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          GuiceCreateInjectorWithCombineRefactor.class, getClass());

  @Test
  public void positive() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Guice;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  private class ModuleB extends AbstractModule {}",
            "  private class ModuleC extends AbstractModule {}",
            "  public void test() {",
            "    Guice.createInjector(",
            "      new ModuleA(),",
            "      Modules.combine(new ModuleB(), new ModuleC()));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Guice;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  private class ModuleB extends AbstractModule {}",
            "  private class ModuleC extends AbstractModule {}",
            "  public void test() {",
            "    Guice.createInjector(",
            "      new ModuleA(),",
            "      new ModuleB(),",
            "      new ModuleC());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_whenNoCombine() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Guice;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  private class ModuleB extends AbstractModule {}",
            "  private class ModuleC extends AbstractModule {}",
            "  public void test() {",
            "    Guice.createInjector(",
            "      new ModuleA(),",
            "      new ModuleB(),",
            "      new ModuleC());",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_whenNoCreateInjector() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  private class ModuleB extends AbstractModule {}",
            "  private class ModuleC extends AbstractModule {}",
            "  public void test() {",
            "    Module extraModule = Modules.combine(",
            "      new ModuleA(),",
            "      new ModuleB(),",
            "      new ModuleC());",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
