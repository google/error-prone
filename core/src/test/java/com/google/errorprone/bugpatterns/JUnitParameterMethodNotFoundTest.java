/*
 * Copyright 2021 The Error Prone Authors.
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

/** Tests for {@link JUnitParameterMethodNotFound} */
@RunWith(JUnit4.class)
public class JUnitParameterMethodNotFoundTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnitParameterMethodNotFound.class, getClass());

  @Test
  public void negativeCase_noErrorsFound() {
    compilationHelper.addSourceFile("JUnitParameterMethodNotFoundNegativeCase.java").doTest();
  }

  @Test
  public void negativeCase_nonJUnitParamsRunner_noErrorsFound() {
    compilationHelper
        .addSourceFile("JUnitParameterMethodNotFoundNegativeCaseNonJUnitParamsRunner.java")
        .doTest();
  }

  @Test
  public void negativeCase_inheritedMethods_noErrorsFound() {
    compilationHelper
        .addSourceFile("JUnitParameterMethodNotFoundNegativeCaseBaseClass.java")
        .addSourceFile("JUnitParameterMethodNotFoundNegativeCaseSuperClass.java")
        .doTest();
  }

  @Test
  public void positiveCase_errorReported() {
    compilationHelper.addSourceFile("JUnitParameterMethodNotFoundPositiveCase.java").doTest();
  }
}
