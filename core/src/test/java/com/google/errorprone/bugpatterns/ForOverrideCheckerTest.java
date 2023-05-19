/*
 * Copyright 2015 The Error Prone Authors.
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

/** Tests for {@code ForOverrideChecker}. */
@RunWith(JUnit4.class)
public class ForOverrideCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ForOverrideChecker.class, getClass())
          .addSourceLines(
              "test/ExtendMe.java",
              "package test;",
              "import com.google.errorprone.annotations.ForOverride;",
              "public class ExtendMe {",
              "  @ForOverride",
              "  protected int overrideMe() { return 1; }",
              "  @ForOverride",
              "  protected int overrideMe(int a) { return 1; }",
              "",
              "  public final void callMe() {",
              "    overrideMe();",
              "  }",
              "}");

  @Test
  public void canApplyForOverrideToProtectedMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import com.google.errorprone.annotations.ForOverride;",
            "public class Test {",
            "  @ForOverride protected void myMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void canApplyForOverrideToPackagePrivateMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import com.google.errorprone.annotations.ForOverride;",
            "public class Test {",
            "  @ForOverride void myMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void cannotApplyForOverrideToPublicMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import com.google.errorprone.annotations.ForOverride;",
            "public class Test {",
            "  // BUG: Diagnostic contains: @ForOverride must have protected or package-private"
                + " visibility",
            "  @ForOverride public void myMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void cannotApplyForOverrideToPrivateMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import com.google.errorprone.annotations.ForOverride;",
            "public class Test {",
            "  // BUG: Diagnostic contains: @ForOverride must have protected or package-private"
                + " visibility",
            "  @ForOverride private void myMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void cannotApplyForOverrideToInterfaceMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import com.google.errorprone.annotations.ForOverride;",
            "public interface Test {",
            "  // BUG: Diagnostic contains: @ForOverride must have protected or package-private"
                + " visibility",
            "  @ForOverride void myMethod();",
            "}")
        .doTest();
  }

  @Test
  public void userCanCallAppropriateMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "public class Test extends test.ExtendMe {",
            "  public void googleyMethod() {",
            "    callMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userInSamePackageCannotCallMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "public class Test {",
            "  public void tryCall() {",
            "    ExtendMe extendMe = new ExtendMe();",
            "    // BUG: Diagnostic contains: must not be invoked",
            "    extendMe.overrideMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCannotCallDefault() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "public class Test extends test.ExtendMe {",
            "  public void circumventer() {",
            "    // BUG: Diagnostic contains: must not be invoked",
            "    overrideMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCannotCallOverridden() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  @Override",
            "  protected int overrideMe() {",
            "    System.err.println(\"Capybaras are semi-aquatic.\");",
            "    return 1;",
            "  }",
            "  public void circumventer() {",
            "    // BUG: Diagnostic contains: must not be invoked",
            "    overrideMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCanCallSuperFromOverridden() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  @Override",
            "  protected int overrideMe() {",
            "    return super.overrideMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCanCallSuperFromOverridden_explicitQualification() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  @Override",
            "  protected int overrideMe() {",
            // This is identical to the above, with a slightly less common explicit qualification
            "    return Test.super.overrideMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCannotCallSuperFromNonOverriddenMethod() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  protected void circumventer() {",
            "    // BUG: Diagnostic contains: must not be invoked",
            "    super.overrideMe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCannotCallSuperFromFieldInitializer() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  // BUG: Diagnostic contains: must not be invoked",
            "  private final int k = super.overrideMe();",
            "}")
        .doTest();
  }

  @Test
  public void userCannotCallSuperFromAnonymousInnerClassInOverride() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  @Override",
            "  protected int overrideMe() {",
            "    return new Object() {",
            "      // BUG: Diagnostic contains: must not be invoked",
            "      final int k = Test.super.overrideMe();",
            "",
            "      int foo() {",
            "        // BUG: Diagnostic contains: must not be invoked",
            "        return Test.super.overrideMe();",
            "      }",
            "    }.foo();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void userCannotMakeMethodPublic() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test2;",
            "public class Test extends test.ExtendMe {",
            "  // BUG: Diagnostic contains: overrides @ForOverride method test.ExtendMe.overrideMe",
            "  public int overrideMe() {",
            "    return 1;",
            "  }",
            "  // BUG: Diagnostic contains: overrides @ForOverride method test.ExtendMe.overrideMe",
            "  public int overrideMe(int a) {",
            "    return 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void definerCanCallFromInnerClass() {
    compilationHelper
        .addSourceLines(
            "test/OuterClass.java",
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
            "}")
        .doTest();
  }

  @Test
  public void definerCanCallFromAnonymousInnerClass() {
    compilationHelper
        .addSourceLines(
            "test/OuterClass.java",
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
            "}")
        .doTest();
  }
}
