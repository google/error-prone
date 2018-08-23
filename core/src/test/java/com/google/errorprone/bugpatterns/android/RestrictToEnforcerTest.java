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

package com.google.errorprone.bugpatterns.android;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test cases for Android's @RestrictTo annotation. */
@RunWith(JUnit4.class)
public final class RestrictToEnforcerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(RestrictToEnforcer.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper
        .addSourceLines(
            "android/support/annotation/RestrictTo.java",
            "package android.support.annotation;",
            "import static java.lang.annotation.ElementType.ANNOTATION_TYPE;",
            "import static java.lang.annotation.ElementType.CONSTRUCTOR;",
            "import static java.lang.annotation.ElementType.FIELD;",
            "import static java.lang.annotation.ElementType.METHOD;",
            "import static java.lang.annotation.ElementType.PACKAGE;",
            "import static java.lang.annotation.ElementType.TYPE;",
            "import java.lang.annotation.Target;",
            "@Target({ANNOTATION_TYPE,TYPE,METHOD,CONSTRUCTOR,FIELD,PACKAGE})",
            "public @interface RestrictTo {",
            "  Scope[] value();",
            "  enum Scope { LIBRARY, LIBRARY_GROUP, GROUP_ID, TESTS, SUBCLASSES }",
            "}")
        .addSourceFile("RestrictToEnforcerPositiveCases.java")
        .addSourceFile("RestrictToEnforcerPositiveCasesApi.java")
        .doTest();
  }
}
