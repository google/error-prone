/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import javax.tools.JavaFileObject;

/**
 * Unit tests for {@link RequiredModifiersChecker}.
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class RequiredModifiersCheckerTest {

  private CompilationTestHelper compilationHelper;

  JavaFileObject abstractRequired;
  JavaFileObject publicAndFinalRequired;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new RequiredModifiersChecker());

    abstractRequired = compilationHelper.fileManager().forSourceLines("test/AbstractRequired.java",
        "package test;",
        "import static javax.lang.model.element.Modifier.ABSTRACT;",
        "import com.google.errorprone.annotations.RequiredModifiers;",
        "@RequiredModifiers(ABSTRACT)",
        "public @interface AbstractRequired {",
        "}");

    publicAndFinalRequired =
        compilationHelper.fileManager().forSourceLines("test/PublicAndFinalRequired.java",
        "package test;",
        "import static javax.lang.model.element.Modifier.FINAL;",
        "import static javax.lang.model.element.Modifier.PUBLIC;",
        "import com.google.errorprone.annotations.RequiredModifiers;",
        "@RequiredModifiers({PUBLIC, FINAL})",
        "public @interface PublicAndFinalRequired {",
        "}");
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnClassFails() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        abstractRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.AbstractRequired;",
            "// BUG: Diagnostic contains: The annotation '@AbstractRequired' has specified that it"
            + " must be used together with the following modifiers: [abstract]",
            "@AbstractRequired public class RequiredModifiersTestCase {",
            "}")));
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnFieldFails1() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        publicAndFinalRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.PublicAndFinalRequired;",
            "public class RequiredModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@PublicAndFinalRequired' has specified"
            + " that it must be used together with the following modifiers: [public, final]",
            "  @PublicAndFinalRequired int n = 0;",
            "}")));
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnFieldFails2() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        publicAndFinalRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.PublicAndFinalRequired;",
            "public class RequiredModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@PublicAndFinalRequired' has specified"
            + " that it must be used together with the following modifiers: [final]",
            "  @PublicAndFinalRequired public int n = 0;",
            "}")));
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnFieldFails3() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        publicAndFinalRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.PublicAndFinalRequired;",
            "public class RequiredModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@PublicAndFinalRequired' has specified"
            + " that it must be used together with the following modifiers: [public]",
            "  @PublicAndFinalRequired final int n = 0;",
            "}")));
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnMethodFails1() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        publicAndFinalRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.PublicAndFinalRequired;",
            "public class RequiredModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@PublicAndFinalRequired' has specified"
            + " that it must be used together with the following modifiers: [public, final]",
            "  @PublicAndFinalRequired private void foo(){}",
            "}")));
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnMethodFails2() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        publicAndFinalRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.PublicAndFinalRequired;",
            "public class RequiredModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@PublicAndFinalRequired' has specified"
            + " that it must be used together with the following modifiers: [final]",
            "  @PublicAndFinalRequired public void foo(){}",
            "}")));
  }

  @Test
  public void testAnnotationWithRequiredModifiersMissingOnMethodFails3() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        publicAndFinalRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.PublicAndFinalRequired;",
            "public class RequiredModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@PublicAndFinalRequired' has specified"
            + " that it must be used together with the following modifiers: [public]",
            "  @PublicAndFinalRequired final void foo(){}",
            "}")));
  }

  @Test
  public void testHasRequiredModifiersSucceeds() throws Exception {
    compilationHelper.assertCompileSucceeds(Arrays.asList(
        abstractRequired,
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.AbstractRequired;",
            "abstract class RequiredModifiersTestCase {}")));
  }

  @Test
  public void testGuavaAnnotation() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(Arrays.asList(
        compilationHelper.fileManager().forSourceLines(
            "com/google/common/annotations/RequiredModifiers.java",
            "package com.google.common.annotations;",
            "import javax.lang.model.element.Modifier;",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target(ElementType.ANNOTATION_TYPE)",
            "public @interface RequiredModifiers {",
            "  Modifier[] value();",
            "}"),
        compilationHelper.fileManager().forSourceLines("test/AbstractRequired.java",
            "package test;",
            "import static javax.lang.model.element.Modifier.ABSTRACT;",
            "import com.google.common.annotations.RequiredModifiers;",
            "@RequiredModifiers(ABSTRACT)",
            "public @interface AbstractRequired {",
            "}"),
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.AbstractRequired;",
            "// BUG: Diagnostic contains: The annotation '@AbstractRequired' has specified that it"
            + " must be used together with the following modifiers: [abstract]",
            "@AbstractRequired public class RequiredModifiersTestCase {",
            "}")));
  }

  @Test
  public void testGuavaAnnotationOK() throws Exception {
    compilationHelper.assertCompileSucceeds(Arrays.asList(
        compilationHelper.fileManager().forSourceLines(
            "com/google/common/annotations/RequiredModifiers.java",
            "package com.google.common.annotations;",
            "import javax.lang.model.element.Modifier;",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target(ElementType.ANNOTATION_TYPE)",
            "public @interface RequiredModifiers {",
            "  Modifier[] value();",
            "}"),
        compilationHelper.fileManager().forSourceLines("test/AbstractRequired.java",
            "package test;",
            "import static javax.lang.model.element.Modifier.ABSTRACT;",
            "import com.google.common.annotations.RequiredModifiers;",
            "@RequiredModifiers(ABSTRACT)",
            "public @interface AbstractRequired {",
            "}"),
        compilationHelper.fileManager().forSourceLines("test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.AbstractRequired;",
            "@AbstractRequired public abstract class RequiredModifiersTestCase {",
            "}")));
  }

  // Regression test for #313
  @Test
  public void negativeNestedAnnotations() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/Test.java",
            "package test;",
            "@interface Foos {",
            "  Foo[] value();",
            "}",
            "@interface Foo {",
            "}",
            "@Foos({@Foo, @Foo}) public class Test {",
            "}"));
  }

  // Regression test for #313
  @Test
  public void negativePackageAnnotation() throws Exception {
    compilationHelper.assertCompileSucceeds(Arrays.asList(
        compilationHelper.fileManager().forSourceLines(
            "testdata/Anno.java",
            "package testdata;",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target(ElementType.PACKAGE)",
            "public @interface Anno {",
            "}"),
        compilationHelper.fileManager().forSourceLines("testdata/package-info.java",
            "@Anno",
            "package testdata;")));
  }
}
