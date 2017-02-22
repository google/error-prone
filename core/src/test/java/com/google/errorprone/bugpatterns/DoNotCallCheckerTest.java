/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** {@link DoNotCallChecker}Test */
@RunWith(JUnit4.class)
public class DoNotCallCheckerTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(DoNotCallChecker.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "class Test {",
            "  @DoNotCall(\"satisfying explanation\") final void f() {}",
            "  @DoNotCall final void g() {}",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    // This method should not be called: satisfying explanation",
            "    f();",
            "    // BUG: Diagnostic contains:",
            "    // This method should not be called, see its documentation for details",
            "    g();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteFinal() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public class Test {",
            "  // BUG: Diagnostic contains: should be final",
            "  @DoNotCall public void f() {}",
            "  @DoNotCall public final void g() {}",
            "}")
        .doTest();
  }

  @Test
  public void requiredOverride() {
    testHelper
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public interface A {",
            "  @DoNotCall public void f();",
            "}")
        .addSourceLines(
            "B.java",
            "public class B implements A {",
            "  // BUG: Diagnostic contains: overrides f in A which is annotated",
            "  @Override public void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void annotatedOverride() {
    testHelper
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public interface A {",
            "  @DoNotCall public void f();",
            "}")
        .addSourceLines(
            "B.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public class B implements A {",
            "  @DoNotCall @Override public final void f() {}",
            "}")
        .doTest();
  }

  // The interface tries to make Object#toString @DoNotCall, and because
  // the declaration in B is implicit it doesn't get checked.
  // In practice, making default Object methods @DoNotCall isn't super
  // useful - typically users interface with the interface directly
  // (e.g. Hasher) or there's an override that has unwanted behaviour (Localizable).
  // TODO(cushon): check class declarations for super-interfaces that do this?
  @Test
  public void interfaceRedeclaresObjectMethod() {
    testHelper
        .addSourceLines(
            "I.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public interface I {",
            "  @DoNotCall public String toString();",
            "}")
        .addSourceLines(
            "B.java", //
            "public class B implements I {",
            "}")
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  void f(B b) {",
            "    b.toString();",
            "    I i = b;",
            "    // BUG: Diagnostic contains:",
            "    i.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public final class Test {",
            "  @DoNotCall public void f() {}",
            "}")
        .doTest();
  }
}
