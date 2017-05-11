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

/** Tests for {@link ProvidesNull}. */
@RunWith(JUnit4.class)
public class ProvidesNullTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ProvidesNull.class, getClass());
  }

  // Positive cases

  @Test
  public void simple() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Provides;",
            "public class Test {",
            "  @Provides public Object providesObject() {",
            "    // BUG: Diagnostic contains: Did you mean '@Nullable' or 'throw new RuntimeException();'",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hasJavaxAnnotationNullable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Provides;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "  @Provides",
            "  @Nullable",
            "  public Object providesObject() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hasOtherNullable() {
    compilationHelper
        .addSourceLines("Nullable.java", "public @interface Nullable {}")
        .addSourceLines(
            "Test.java",
            "import dagger.Provides;",
            "public class Test {",
            "  @Provides",
            "  @Nullable",
            "  public Object providesObject() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Tests that we do not flag Guice {@code @Provides} methods. While this is also wrong, there is
   * no enforcement in Guice and so the incorrect usage is too common to error on.
   */
  @Test
  public void guiceProvides() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.inject.Provides;",
            "public class Test {",
            "  @Provides",
            "  public Object providesObject() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import dagger.Provides;",
            "public class Test {",
            "  @Provides public Object providesObject() {",
            "    try {",
            "      return new Object();",
            "    } catch (Exception e) {",
            "      // BUG: Diagnostic contains: Did you mean 'throw new RuntimeException(e);' or '@Nullable'",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inTry() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Provides;",
            "public class Test {",
            "  @Provides public Object providesObject() {",
            "    try {",
            "      // BUG: Diagnostic contains: Did you mean '@Nullable' or 'throw new RuntimeException();'",
            "      return null;",
            "    } catch (Exception e) {",
            "      return new Object();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnWithNoExpression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import dagger.Provides;",
            "public class Test {",
            "  public void doNothing() {",
            "    return;",
            "  }",
            "}")
        .doTest();
  }
}
