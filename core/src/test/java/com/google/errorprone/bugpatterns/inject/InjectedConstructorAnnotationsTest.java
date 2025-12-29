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

/** A test for InjectedConstructorAnnotations */
@RunWith(JUnit4.class)
public class InjectedConstructorAnnotationsTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InjectedConstructorAnnotations.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "InjectedConstructorAnnotationsPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import com.google.inject.BindingAnnotation;
            import com.google.inject.Inject;

            /** A positive test case for InjectedConstructorAnnotation. */
            public class InjectedConstructorAnnotationsPositiveCases {

              /** A binding annotation. */
              @BindingAnnotation
              public @interface TestBindingAnnotation {}

              /** A class with an optionally injected constructor. */
              public class TestClass1 {
                @Inject(optional = true)
                // BUG: Diagnostic contains:
                public TestClass1() {}
              }

              /** A class with an injected constructor that has a binding annotation. */
              public class TestClass2 {
                @TestBindingAnnotation
                @Inject
                // BUG: Diagnostic contains:
                public TestClass2() {}
              }

              /** A class whose constructor is optionally injected and has a binding annotation. */
              public class TestClass3 {
                @TestBindingAnnotation
                @Inject(optional = true)
                // BUG: Diagnostic contains:
                public TestClass3() {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "InjectedConstructorAnnotationsNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import com.google.inject.BindingAnnotation;
            import com.google.inject.Inject;

            /** A negative test case for InjectedConstructorAnnotation. */
            public class InjectedConstructorAnnotationsNegativeCases {

              private @interface TestAnnotation {}

              @BindingAnnotation
              private @interface TestBindingAnnotation {}

              /** A class with a constructor that has no annotations. */
              public class TestClass1 {
                public TestClass1() {}
              }

              /** A class with a constructor that has a binding Annotation. */
              public class TestClass2 {
                @TestBindingAnnotation
                public TestClass2() {}
              }

              /** A class with an injected constructor. */
              public class TestClass3 {
                @Inject
                public TestClass3() {}
              }

              /** A class with an injected constructor that has a non-binding annotation. */
              public class TestClass4 {
                @Inject
                @TestAnnotation
                public TestClass4() {}
              }
            }
            """)
        .doTest();
  }
}
