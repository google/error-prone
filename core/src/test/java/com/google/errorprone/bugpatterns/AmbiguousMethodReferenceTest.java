/*
 * Copyright 2016 The Error Prone Authors.
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

/** {@link AmbiguousMethodReference}Test */
@RunWith(JUnit4.class)
public class AmbiguousMethodReferenceTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(AmbiguousMethodReference.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  // BUG: Diagnostic contains: c(A, D)",
            "  B c(D d) {",
            "    return null;",
            "  }",
            "  static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void moreThan1PublicMethod() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  // BUG: Diagnostic contains: c(A, D)",
            "  public B c(D d) {",
            "    return null;",
            "  }",
            "  public static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedAtClass() {
    testHelper
        .addSourceLines(
            "A.java", //
            "@SuppressWarnings(\"AmbiguousMethodReference\")",
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  B c(D d) {",
            "    return null;",
            "  }",
            "  static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedAtMethod() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  @SuppressWarnings(\"AmbiguousMethodReference\")",
            "  B c(D d) {",
            "    return null;",
            "  }",
            "  // BUG: Diagnostic contains: c(D)",
            "  static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedAtBothMethods() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  @SuppressWarnings(\"AmbiguousMethodReference\")",
            "  B c(D d) {",
            "    return null;",
            "  }",
            "  @SuppressWarnings(\"AmbiguousMethodReference\")",
            "  static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeDifferentNames() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  B c(D d) {",
            "    return null;",
            "  }",
            "  static B d(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativePrivateMethods() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  private B c(D d) {",
            "    return null;",
            "  }",
            "  private static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void only1PublicMethod() {
    testHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  interface B {}",
            "  interface C {}",
            "  interface D {}",
            "",
            "  private B c(D d) {",
            "    return null;",
            "  }",
            "  public static B c(A a, D d) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStatic() {
    testHelper
        .addSourceLines(
            "B.java", //
            "public interface B<T> {",
            "  static <T> B<T> f() { return null; }",
            "}")
        .addSourceLines(
            "A.java", //
            "public abstract class A<T> implements B<T> {",
            "  public static <T> A<T> f() { return null; }",
            "}")
        .doTest();
  }
}
