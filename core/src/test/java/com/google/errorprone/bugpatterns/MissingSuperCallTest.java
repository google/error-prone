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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MissingSuperCall}. */
@RunWith(JUnit4.class)
public class MissingSuperCallTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingSuperCall.class, getClass())
          .addSourceLines(
              "android/support/annotation/CallSuper.java",
              "package android.support.annotation;",
              "import static java.lang.annotation.ElementType.METHOD;",
              "import java.lang.annotation.Target;",
              "@Target({METHOD})",
              "public @interface CallSuper {}");

  @Test
  public void android() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import android.support.annotation.CallSuper;",
            "public class Super {",
            "  @CallSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with @CallSuper,",
            "  // but does not call the super method",
            "  @Override public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void javax() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "public class Super {",
            "  @OverridingMethodsMustInvokeSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with",
            "  // @OverridingMethodsMustInvokeSuper, but does not call the super method",
            "  @Override public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void errorProne() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;",
            "public class Super {",
            "  @OverridingMethodsMustInvokeSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with",
            "  // @OverridingMethodsMustInvokeSuper, but does not call the super method",
            "  @Override public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void findBugs() {
    compilationHelper
        .addSourceLines(
            "edu/umd/cs/findbugs/annotations/OverrideMustInvoke.java",
            "package edu.umd.cs.findbugs.annotations;",
            "public @interface OverrideMustInvoke {}")
        .addSourceLines(
            "Super.java",
            "import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;",
            "public class Super {",
            "  @OverrideMustInvoke public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with @OverrideMustInvoke,",
            "  // but does not call the super method",
            "  @Override public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeDoesCallSuper() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import android.support.annotation.CallSuper;",
            "public class Super {",
            "  @CallSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  @Override public void doIt() {",
            "    super.doIt();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeDoesCallSuper2() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import android.support.annotation.CallSuper;",
            "public class Super {",
            "  @CallSuper public Object doIt() {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "Sub.java",
            "import java.util.Objects;",
            "public class Sub extends Super {",
            "  @Override public Object doIt() {",
            "    return Objects.requireNonNull(super.doIt());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTwoLevelsApart() {
    compilationHelper
        .addSourceLines(
            "a/b/c/SuperSuper.java",
            "package a.b.c;",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "public class SuperSuper {",
            "  @OverridingMethodsMustInvokeSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Super.java", //
            "import a.b.c.SuperSuper;",
            "public class Super extends SuperSuper {}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides a.b.c.SuperSuper#doIt, which is annotated with",
            "  // @OverridingMethodsMustInvokeSuper, but does not call the super method",
            "  @Override public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void androidAnnotationDisallowedOnAbstractMethod() {
    compilationHelper
        .addSourceLines(
            "AbstractClass.java",
            "import android.support.annotation.CallSuper;",
            "public abstract class AbstractClass {",
            "  // BUG: Diagnostic contains: @CallSuper cannot be applied to an abstract method",
            "  @CallSuper public abstract void bad();",
            "  @CallSuper public void ok() {}",
            "}")
        .doTest();
  }

  @Test
  public void javaxAnnotationDisallowedOnAbstractMethod() {
    compilationHelper
        .addSourceLines(
            "AbstractClass.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "public abstract class AbstractClass {",
            "  // BUG: Diagnostic contains:",
            "  // @OverridingMethodsMustInvokeSuper cannot be applied to an abstract method",
            "  @OverridingMethodsMustInvokeSuper public abstract void bad();",
            "  @OverridingMethodsMustInvokeSuper public void ok() {}",
            "}")
        .doTest();
  }

  @Test
  public void findBugsAnnotationDisallowedOnAbstractMethod() {
    compilationHelper
        .addSourceLines(
            "edu/umd/cs/findbugs/annotations/OverrideMustInvoke.java",
            "package edu.umd.cs.findbugs.annotations;",
            "public @interface OverrideMustInvoke {}")
        .addSourceLines(
            "AbstractClass.java",
            "import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;",
            "public abstract class AbstractClass {",
            "  // BUG: Diagnostic contains:",
            "  // @OverrideMustInvoke cannot be applied to an abstract method",
            "  @OverrideMustInvoke public abstract void bad();",
            "  @OverrideMustInvoke public void ok() {}",
            "}")
        .doTest();
  }

  @Test
  public void annotationDisallowedOnInterfaceMethod() {
    compilationHelper
        .addSourceLines(
            "MyInterface.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "interface MyInterface {",
            "  // BUG: Diagnostic contains:",
            "  // @OverridingMethodsMustInvokeSuper cannot be applied to an abstract method",
            "  @OverridingMethodsMustInvokeSuper void bad();",
            "  @OverridingMethodsMustInvokeSuper default void ok() {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveOverridingDefaultInterfaceMethod() {
    compilationHelper
        .addSourceLines(
            "MyInterface.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "interface MyInterface {",
            "  @OverridingMethodsMustInvokeSuper default void doIt() {}",
            "}")
        .addSourceLines(
            "MyImplementation.java",
            "public class MyImplementation implements MyInterface {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides MyInterface#doIt, which is annotated with",
            "  // @OverridingMethodsMustInvokeSuper, but does not call the super method",
            "  @Override public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void superAndSubAnnotated() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "public class Super {",
            "  @OverridingMethodsMustInvokeSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "public class Sub extends Super {",
            "  @OverridingMethodsMustInvokeSuper",
            "  @Override",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with",
            "  // @OverridingMethodsMustInvokeSuper, but does not call the super method",
            "  public void doIt() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeOverridingMethodIsAbstract() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import javax.annotation.OverridingMethodsMustInvokeSuper;",
            "public class Super {",
            "  @OverridingMethodsMustInvokeSuper public void doIt() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public abstract class Sub extends Super {",
            "  @Override public abstract void doIt();",
            "}")
        .doTest();
  }

  @Test
  public void wrongSuperCall() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import android.support.annotation.CallSuper;",
            "public class Super {",
            "  @CallSuper public void doIt() {}",
            "  public void wrongToCall() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with @CallSuper,",
            "  // but does not call the super method",
            "  @Override public void doIt() {",
            "    super.wrongToCall();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedSuperCall() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import android.support.annotation.CallSuper;",
            "public class Super {",
            "  @CallSuper public void doIt() {}",
            "  public void wrongToCall() {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "public class Sub extends Super {",
            "  // BUG: Diagnostic contains:",
            "  // This method overrides Super#doIt, which is annotated with @CallSuper,",
            "  // but does not call the super method",
            "  @Override public void doIt() {",
            "    new Super() {",
            "      @Override public void doIt() {",
            "        super.doIt();",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
