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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@RunWith(JUnit4.class)
public class ScopeOrQualifierAnnotationRetentionTest {

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          ScopeOrQualifierAnnotationRetention.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ScopeOrQualifierAnnotationRetention.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceFile("ScopeOrQualifierAnnotationRetentionPositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceFile("ScopeOrQualifierAnnotationRetentionNegativeCases.java")
        .doTest();
  }

  @Test
  public void testRefactoring() {
    refactoringTestHelper
        .addInputLines(
            "in/Anno.java",
            "import static java.lang.annotation.ElementType.METHOD;",
            "import static java.lang.annotation.ElementType.TYPE;",
            "",
            "import java.lang.annotation.Target;",
            "import javax.inject.Qualifier;",
            "",
            "@Qualifier",
            "@Target({TYPE, METHOD})",
            "public @interface Anno {}")
        .addOutputLines(
            "out/Anno.java",
            "import static java.lang.annotation.ElementType.METHOD;",
            "import static java.lang.annotation.ElementType.TYPE;",
            "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
            "",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.Target;",
            "import javax.inject.Qualifier;",
            "",
            "@Qualifier",
            "@Target({TYPE, METHOD})",
            "@Retention(RUNTIME)",
            "public @interface Anno {}")
        .doTest();
  }

  @Test
  public void nestedQualifierInDaggerModule() {
    compilationHelper
        .addSourceLines(
            "DaggerModule.java", //
            "@dagger.Module class DaggerModule {",
            "@javax.inject.Scope",
            "public @interface TestAnnotation {}",
            "}")
        .doTest();
  }

  @Test
  public void testIgnoredOnAndroid() {
    compilationHelper
        .setArgs(Collections.singletonList("-XDandroidCompatible=true"))
        .addSourceLines(
            "TestAnnotation.java", //
            "@javax.inject.Scope",
            "public @interface TestAnnotation {}")
        .doTest();
  }

  @Test
  public void testSourceRetentionStillFiringOnAndroid() {
    compilationHelper
        .setArgs(Collections.singletonList("-XDandroidCompatible=true"))
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "@javax.inject.Scope",
            "// BUG: Diagnostic contains: @Retention(RUNTIME)",
            "@Retention(RetentionPolicy.SOURCE)",
            "public @interface TestAnnotation {}")
        .doTest();
  }
}
