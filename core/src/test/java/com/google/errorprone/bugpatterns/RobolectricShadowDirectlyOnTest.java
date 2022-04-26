/*
 * Copyright 2022 The Error Prone Authors.
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

/** {@link RobolectricShadowDirectlyOn}Test */
@RunWith(JUnit4.class)
public class RobolectricShadowDirectlyOnTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(RobolectricShadowDirectlyOn.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "Shadow.java",
            "package org.robolectric.shadow.api;",
            "import org.robolectric.util.ReflectionHelpers.ClassParameter;",
            "public class Shadow {",
            "  public static <T> T directlyOn(T shadowedObject, Class<T> clazz) {",
            "    return null;",
            "  }",
            "  public static <T> Runnable directlyOn(",
            "          T shadowedObject, Class<T> clazz, String baz, ClassParameter<?>... params)"
                + " {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "ReflectionHelpers.java",
            "package org.robolectric.util;",
            "public class ReflectionHelpers {",
            "  public static class ClassParameter<V> {",
            "    public static <V> ClassParameter<V> from(Class<? extends V> clazz, V val) {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Foo.java",
            "import java.util.List;",
            "class Foo {",
            "  Runnable baz(Object r, long n, List<?> x, String s) {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "import java.util.List;",
            "import org.robolectric.shadow.api.Shadow;",
            "class Test {",
            "  public <T> Runnable registerNativeAllocation(Foo foo, Object r, long n, List<T> x)"
                + " {",
            "    return Shadow.directlyOn(foo, Foo.class).baz(r, n, x, null);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.List;",
            "import org.robolectric.shadow.api.Shadow;",
            "import org.robolectric.util.ReflectionHelpers.ClassParameter;",
            "class Test {",
            "  public <T> Runnable registerNativeAllocation(Foo foo, Object r, long n, List<T> x)"
                + " {",
            "  return Shadow.directlyOn(",
            "          foo,",
            "          Foo.class,",
            "          \"baz\",",
            "          ClassParameter.from(Object.class, r),",
            "          ClassParameter.from(long.class, n),",
            "          ClassParameter.from(List.class, x),",
            "          ClassParameter.from(String.class, null));",
            "  }",
            "}")
        .doTest();
  }
}
