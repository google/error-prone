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

package com.google.errorprone.bugpatterns.inject;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class ScopeAnnotationOnInterfaceOrAbstractClassTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(
          ScopeAnnotationOnInterfaceOrAbstractClass.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ScopeAnnotationOnInterfaceOrAbstractClassPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.inject.testdata;

import com.google.inject.Singleton;

/**
 * Positive test cases in which a scoping annotation is put on an interface or anabstract class. The
 * suggested fix is to remove the scoping annotation.
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class ScopeAnnotationOnInterfaceOrAbstractClassPositiveCases {

  /** An abstract class that has scoping annotation. */
  // BUG: Diagnostic contains: remove
  @Singleton
  public abstract class TestClass1 {}

  /** An interface interface has scoping annotation. */
  // BUG: Diagnostic contains: remove
  @Singleton
  public interface TestClass2 {}
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ScopeAnnotationOnInterfaceOrAbstractClassNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import com.google.inject.Singleton;
            import dagger.Component;
            import dagger.Subcomponent;
            import javax.inject.Scope;

            /**
             * Negative test cases in which scoping annotations are used correctly.
             *
             * @author sgoldfeder@google.com (Steven Goldfeder)
             */
            public class ScopeAnnotationOnInterfaceOrAbstractClassNegativeCases {

              /** A concrete class that has no scoping annotation. */
              public class TestClass1 {}

              /** An abstract class that has no scoping annotation. */
              public abstract class TestClass2 {}

              /** An interface that has no scoping annotation. */
              public interface TestClass3 {}

              /** A concrete class that has scoping annotation. */
              @Singleton
              public class TestClass4 {}

              @Scope
              @interface CustomScope {}

              /** A concrete class that has a custom annotation. */
              @CustomScope
              public class ClassWithCustomScope {}

              @Component
              @Singleton
              interface DaggerInterfaceComponent {
                @Subcomponent
                @CustomScope
                abstract class DaggerAbstractClassSubcomponent {}
              }
            }\
            """)
        .doTest();
  }
}
