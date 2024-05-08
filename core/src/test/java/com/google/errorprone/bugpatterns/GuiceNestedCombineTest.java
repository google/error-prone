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
public final class GuiceNestedCombineTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(GuiceNestedCombine.class, getClass());

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
  public void arbitraryVarargsMethod_combineCollapsed() {
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
            "    foo(",
            "      new ModuleA(),",
            "      Modules.combine(new ModuleB(), new ModuleC()));",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  private class ModuleB extends AbstractModule {}",
            "  private class ModuleC extends AbstractModule {}",
            "  public void test() {",
            "    foo(",
            "      new ModuleA(),",
            "      new ModuleB(), new ModuleC());",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .doTest();
  }

  @Test
  public void singleArgument_collapsed() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  public void test() {",
            "    foo(",
            "      new ModuleA(),",
            "      Modules.combine(new ModuleA()));",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  public void test() {",
            "    foo(",
            "      new ModuleA(),",
            "      new ModuleA());",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .doTest();
  }

  @Test
  public void noArguments_ignored() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  public void test() {",
            "    foo(",
            "      new ModuleA(),",
            "      Modules.combine());",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void iterableOverload_noFinding() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  public void test() {",
            "    foo(",
            "      new ModuleA(),",
            "      Modules.combine(ImmutableList.of(new ModuleA())));",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void varargsMethod_arrayInputToCombine_noFinding() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  public void test(Module[] ms) {",
            "    foo(Modules.combine(ms));",
            "  }",
            "  public void foo(Module... xs) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void notVargs_noFinding() {
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
            "    foo(",
            "      new ModuleA(),",
            "      Modules.combine(new ModuleB(), new ModuleC()));",
            "  }",
            "  public void foo(Module a, Module b) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void partialVarargs_collapsed() {
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
            "    foo(",
            "      new ModuleA(),",
            "      Modules.combine(new ModuleB(), new ModuleC()),",
            "      Modules.combine(new ModuleB(), new ModuleC()));",
            "  }",
            "  public void foo(Module a, Module... b) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "import com.google.inject.Module;",
            "import com.google.inject.util.Modules;",
            "class Test {",
            "  private class ModuleA extends AbstractModule {}",
            "  private class ModuleB extends AbstractModule {}",
            "  private class ModuleC extends AbstractModule {}",
            "  public void test() {",
            "    foo(",
            "      new ModuleA(),",
            "      new ModuleB(), new ModuleC(),",
            "      new ModuleB(), new ModuleC());",
            "  }",
            "  public void foo(Module a, Module... b) {}",
            "}")
        .doTest();
  }

  @Test
  public void noCombine_noFinding() {
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
  public void assignedToVariable_noFinding() {
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
