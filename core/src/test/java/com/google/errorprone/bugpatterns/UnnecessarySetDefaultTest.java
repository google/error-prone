/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.testing.ArbitraryInstances;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnnecessarySetDefault}Test */
@RunWith(JUnit4.class)
public class UnnecessarySetDefaultTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessarySetDefault(), getClass());

  @Test
  public void refactoring() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.testing.NullPointerTester;",
            "class Test {",
            "  {",
            "    NullPointerTester tester = new NullPointerTester();",
            "    tester.setDefault(String.class, \"\");",
            "    tester",
            "        .setDefault(ImmutableList.class, ImmutableList.of(42))",
            "        .setDefault(ImmutableList.class, ImmutableList.of())",
            "        .setDefault(ImmutableList.class, ImmutableList.<String>of())",
            "        .setDefault(ImmutableList.class, ImmutableList.of(42));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.testing.NullPointerTester;",
            "class Test {",
            "  {",
            "    NullPointerTester tester = new NullPointerTester();",
            "    tester",
            "        .setDefault(ImmutableList.class, ImmutableList.of(42))",
            "        .setDefault(ImmutableList.class, ImmutableList.of(42));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void exhaustive() throws ReflectiveOperationException {
    Field f = ArbitraryInstances.class.getDeclaredField("DEFAULTS");
    f.setAccessible(true);
    ClassToInstanceMap<?> actual = (ClassToInstanceMap<?>) f.get(null);
    assertThat(UnnecessarySetDefault.DEFAULTS.keySet())
        .containsAnyIn(
            actual.keySet().stream().map(Class::getCanonicalName).collect(toImmutableList()));
  }
}
