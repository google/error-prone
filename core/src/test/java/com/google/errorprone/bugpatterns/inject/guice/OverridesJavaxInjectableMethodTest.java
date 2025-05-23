/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.guice;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class OverridesJavaxInjectableMethodTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(OverridesJavaxInjectableMethod.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "OverridesJavaxInjectableMethodPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.inject.guice.testdata;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class OverridesJavaxInjectableMethodPositiveCases {

  /** Class with foo() */
  public class TestClass0 {
    public void foo() {}
  }

  /**
   * Class with a method foo() that is annotated with {@code javax.inject.Inject}. Other test
   * classes will extend this class.
   */
  public class TestClass1 extends TestClass0 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method annotated with
   * {@code @}javax.inject.Inject.
   */
  public class TestClass2 extends TestClass1 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method that in turn overrides
   * a method that is annotated with @javax.inject.Inject.
   */
  public class TestClass3 extends TestClass2 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "OverridesJavaxInjectableMethodNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.guice.testdata;

            /**
             * @author sgoldfeder@google.com (Steven Goldfeder)
             */
            public class OverridesJavaxInjectableMethodNegativeCases {
              /** Class with a method foo() with no annotations. */
              public class TestClass1 {
                public void foo() {}
              }

              /** Class with a method foo() annotated with @com.google.inject.Inject. */
              public class TestClass2 {
                @com.google.inject.Inject
                public void foo() {}
              }

              /** Class with a method foo() annotated with @javax.inject.Inject. */
              public class TestClass3 {
                @javax.inject.Inject
                public void foo() {}
              }

              /** OK, as it overrides a Guice-Inject method */
              public class TestClass4 extends TestClass2 {
                @Override
                public void foo() {}
              }

              /** gInject <- jInject */
              public class TestClass5 extends TestClass3 {
                @com.google.inject.Inject
                public void foo() {}
              }

              /** jInject <- gInject */
              public class TestClass6 extends TestClass2 {
                @javax.inject.Inject
                public void foo() {}
              }

              /** OK, as 7 <- jInject <- gInject */
              public class TestClass7 extends TestClass6 {
                public void foo() {}
              }

              /** OK, as 8 <- gInject */
              public class TestClass8 extends TestClass5 {
                public void foo() {}
              }

              /** Explicitly suppressed warning */
              public class TestClass9 extends TestClass3 {
                @Override
                @SuppressWarnings("OverridesJavaxInjectableMethod")
                public void foo() {}
              }
            }\
            """)
        .doTest();
  }
}
