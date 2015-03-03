/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import javax.tools.JavaFileObject;

/**
 * Tests for {@code ForOverrideChecker}.
 */
@RunWith(JUnit4.class)
public class ForOverrideCheckerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() throws Exception {
    compilationHelper = CompilationTestHelper.newInstance(new ForOverrideChecker());
  }

  private List<JavaFileObject> withFile(String name, String... lines) {

    JavaFileObject extendedClass = compilationHelper.fileManager().forSourceLines(
        "test/ExtendMe.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",

        "public class ExtendMe {",
        "  @ForOverride",
        "  protected void overrideMe() {}",
        "",
        "  public final void callMe() {",
        "    overrideMe();",
        "  }",
        "}");

    return ImmutableList.of(extendedClass,
        compilationHelper.fileManager().forSourceLines(name, lines));
  }

  @Test
  public void testCanApplyForOverrideToProtectedMethod() throws Exception {
    compilationHelper.assertCompileSucceeds(withFile("test/Test.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",
        "public class Test {",
        "  @ForOverride protected void myMethod() {}",
        "}"));
  }

  @Test
  public void testCanApplyForOverrideToPackagePrivateMethod() throws Exception {
    compilationHelper.assertCompileSucceeds(withFile("test/Test.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",
        "public class Test {",
        "  @ForOverride void myMethod() {}",
        "}"));
  }

  @Test
  public void testCannotApplyForOverrideToPublicMethod() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",
        "public class Test {",
        "  // BUG: Diagnostic contains: must have protected or package-private visibility",
        "  @ForOverride public void myMethod() {}",
        "}"));
  }

  @Test
  public void testCannotApplyForOverrideToPrivateMethod() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",
        "public class Test {",
        "  // BUG: Diagnostic contains: must have protected or package-private visibility",
        "  @ForOverride private void myMethod() {}",
        "}"));
  }

  @Test
  public void testCannotApplyForOverrideToInterfaceMethod() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",
        "public interface Test {",
        "  // BUG: Diagnostic contains: must have protected or package-private visibility",
        "  @ForOverride void myMethod();",
        "}"));
  }

  @Test
  public void testUserCanCallAppropriateMethod() throws Exception {
    compilationHelper.assertCompileSucceeds(withFile("test/Test.java",
        "package test;",
        "public class Test extends test.ExtendMe {",
        "  public void googleyMethod() {",
        "    callMe();",
        "  }",
        "}"));
  }

  @Test
  public void testUserInSamePackageCannotCallMethod() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test;",
        "public class Test {",
        "  public void tryCall() {",
        "    ExtendMe extendMe = new ExtendMe();",
        "    // BUG: Diagnostic contains: must not be invoked",
        "    extendMe.overrideMe();",
        "  }",
        "}"));
  }

  @Test
  public void testUserCannotCallDefault() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test;",
        "public class Test extends test.ExtendMe {",
        "  public void circumventer() {",
        "    // BUG: Diagnostic contains: must not be invoked",
        "    overrideMe();",
        "  }",
        "}"));
  }

  @Test
  public void testUserCannotCallOverridden() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test2;",
        "public class Test extends test.ExtendMe {",
        "  @Override",
        "  protected void overrideMe() {",
        "    System.err.println(\"Capybaras are semi-aquatic.\");",
        "  }",
        "  public void circumventer() {",
        "    // BUG: Diagnostic contains: must not be invoked",
        "    overrideMe();",
        "  }",
        "}"));
  }

  @Test
  public void testUserCannotMakeMethodPublic() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(withFile("test/Test.java",
        "package test2;",
        "public class Test extends test.ExtendMe {",
        "  // BUG: Diagnostic contains: must have protected or package-private visibility",
        "  public void overrideMe() {",
        "    System.err.println(\"Capybaras are rodents.\");",
        "  }",
        "}"));
  }

  @Test
  public void testDefinerCanCallFromInnerClass() throws Exception {
    compilationHelper.assertCompileSucceeds(withFile("test/OuterClass.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",

        "public class OuterClass {",
        "  @ForOverride",
        "  protected void forOverride() { }",

        "  private class InnerClass {",
        "    void invoke() {",
        "      forOverride();",
        "    }",
        "  }",
        "}"));
  }

  @Test
  public void testDefinerCanCallFromAnonymousInnerClass() throws Exception {
    compilationHelper.assertCompileSucceeds(withFile("test/OuterClass.java",
        "package test;",
        "import com.google.errorprone.annotations.ForOverride;",

        "public class OuterClass {",
        "  @ForOverride",
        "  protected void forOverride() { }",

        "  public Runnable getRunner() {",
        "    return new Runnable() {",
        "      public void run() {",
        "        forOverride();",
        "      }",
        "    };",
        "  }",
        "}"));
  }
}
