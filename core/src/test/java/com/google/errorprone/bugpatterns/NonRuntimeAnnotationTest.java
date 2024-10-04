/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
@RunWith(JUnit4.class)
public class NonRuntimeAnnotationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NonRuntimeAnnotation.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "NonRuntimeAnnotationPositiveCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
@NonRuntimeAnnotationPositiveCases.NotSpecified
@NonRuntimeAnnotationPositiveCases.NonRuntime
public class NonRuntimeAnnotationPositiveCases {

  public NonRuntime testAnnotation() {
    // BUG: Diagnostic contains: runtime; NonRuntime
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NonRuntime.class);
    // BUG: Diagnostic contains:
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NotSpecified.class);
    // BUG: Diagnostic contains:
    return this.getClass().getAnnotation(NonRuntimeAnnotationPositiveCases.NonRuntime.class);
  }

  /** Annotation that is explicitly NOT retained at runtime */
  @Retention(RetentionPolicy.SOURCE)
  public @interface NonRuntime {}

  /** Annotation that is implicitly NOT retained at runtime */
  public @interface NotSpecified {}
}""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "NonRuntimeAnnotationNegativeCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
@NonRuntimeAnnotationNegativeCases.Runtime
public class NonRuntimeAnnotationNegativeCases {

  public Runtime testAnnotation() {
    return this.getClass().getAnnotation(NonRuntimeAnnotationNegativeCases.Runtime.class);
  }

  /** Annotation that is retained at runtime */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Runtime {}
}""")
        .doTest();
  }
}
