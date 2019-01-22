/*
 * Copyright 2019 The Error Prone Authors.
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
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link RefersToDaggerCodegen}. */
@RunWith(JUnit4.class)
public class RefersToDaggerCodegenTest {
  private static final String PACKAGE_NAME = InjectedClass.class.getPackage().getName();
  private static final String FULLY_QUALIFIED_FACTORY_NAME =
      PACKAGE_NAME + ".RefersToDaggerCodegenTest_InjectedClass_Factory";

  private CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(RefersToDaggerCodegen.class, getClass());

  static class InjectedClass {
    @Inject
    InjectedClass() {}
  }

  public static class LooksLike_Factory {
    public static LooksLike_Factory create() {
      return new LooksLike_Factory();
    }
  }

  public static class MembersInjectedClass {
    @Inject Object object;
  }

  @Test
  public void testPositiveCase() {
    compilationTestHelper
        .addSourceLines(
            "in/TestClass.java",
            "import static " + FULLY_QUALIFIED_FACTORY_NAME + ".create;",
            "import " + FULLY_QUALIFIED_FACTORY_NAME + ";",
            "",
            "class TestClass {",
            "  void TestClass() {",
            "    // BUG: Diagnostic contains: Dagger's internal or generated code",
            "    RefersToDaggerCodegenTest_InjectedClass_Factory.create();",
            "",
            "    // BUG: Diagnostic contains: Dagger's internal or generated code",
            "    " + FULLY_QUALIFIED_FACTORY_NAME + ".create();",
            "",
            "    // BUG: Diagnostic contains: Dagger's internal or generated code",
            "    create();",
            "",
            "    // BUG: Diagnostic contains: Dagger's internal or generated code",
            "    RefersToDaggerCodegenTest_InjectedClass_Factory.newInstance();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void okToReferenceInDaggerCodegen() {
    String generatedAnnotationName;
    try {
      Class.forName("java.lang.Module");
      generatedAnnotationName = "javax.annotation.processing.Generated";
    } catch (ClassNotFoundException e) {
      generatedAnnotationName = "javax.annotation.Generated";
    }
    compilationTestHelper
        .addSourceLines(
            "in/TestClass.java",
            "import static " + FULLY_QUALIFIED_FACTORY_NAME + ".create;",
            "import " + FULLY_QUALIFIED_FACTORY_NAME + ";",
            "",
            "@" + generatedAnnotationName + "(\"dagger.internal.codegen.ComponentProcessor\")",
            "class TestClass {",
            "  void TestClass() {",
            "    RefersToDaggerCodegenTest_InjectedClass_Factory.create();",
            "",
            "    " + FULLY_QUALIFIED_FACTORY_NAME + ".create();",
            "",
            "    create();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dontErrorOnCodeThatLooksLikeDaggerGeneration() {
    String looksLikeFactoryFullyQualifiedName =
        PACKAGE_NAME + ".RefersToDaggerCodegenTest.LooksLike_Factory";
    compilationTestHelper
        .addSourceLines(
            "in/TestClass.java",
            "import static " + looksLikeFactoryFullyQualifiedName + ".create;",
            "import " + looksLikeFactoryFullyQualifiedName + ";",
            "",
            "class TestClass {",
            "  void TestClass() {",
            "    LooksLike_Factory.create();",
            "",
            "    " + looksLikeFactoryFullyQualifiedName + ".create();",
            "",
            "    create();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void membersInjector() {
    compilationTestHelper
        .addSourceLines(
            "in/TestClass.java",
            "import dagger.MembersInjector;",
            "import " + PACKAGE_NAME + ".RefersToDaggerCodegenTest;",
            "import "
                + PACKAGE_NAME
                + ".RefersToDaggerCodegenTest_MembersInjectedClass_MembersInjector;",
            "",
            "class TestClass {",
            "  void TestClass() {",
            "    // BUG: Diagnostic contains: Dagger's internal or generated code",
            "    RefersToDaggerCodegenTest_MembersInjectedClass_MembersInjector.create(null);",
            "",
            "    MembersInjector<RefersToDaggerCodegenTest.MembersInjectedClass> membersInjector",
            "        = null;",
            "    membersInjector.injectMembers(null);", // allowed
            "  }",
            "}")
        .doTest();
  }
}
