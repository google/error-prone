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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author friedj@google.com (Jake Fried) */
@RunWith(JUnit4.class)
public class RxReturnValueIgnoredTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RxReturnValueIgnored.class, getClass())
          // Rx1 stubs
          .addSourceLines(
              "rx1/Observable.java", //
              "package rx;", //
              "public class Observable<T> {}" //
              )
          .addSourceLines(
              "rx1/Single.java", //
              "package rx;", //
              "public class Single<T> {}" //
              )
          .addSourceLines(
              "rx1/Completable.java", //
              "package rx;", //
              "public class Completable<T> {}" //
              )
          // Rx2 stubs
          .addSourceLines(
              "rx2/Observable.java", //
              "package io.reactivex;", //
              "public class Observable<T> {}" //
              )
          .addSourceLines(
              "rx2/Single.java", //
              "package io.reactivex;", //
              "public class Single<T> {}" //
              )
          .addSourceLines(
              "rx2/Completable.java", //
              "package io.reactivex;", //
              "public class Completable<T> {}" //
              )
          .addSourceLines(
              "rx2/Maybe.java", //
              "package io.reactivex;", //
              "public class Maybe<T> {}" //
              )
          .addSourceLines(
              "rx2/Flowable.java", //
              "package io.reactivex;", //
              "public class Flowable<T> {}" //
              );

  @Test
  public void positiveCases() {
    compilationHelper.addSourceFile("RxReturnValueIgnoredPositiveCases.java").doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper.addSourceFile("RxReturnValueIgnoredNegativeCases.java").doTest();
  }

  @Test
  public void rx2Observable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import io.reactivex.Observable;",
            "class Test {",
            "  Observable getObservable() { return null; }",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getObservable();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx2Single() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import io.reactivex.Single;",
            "class Test {",
            "  Single getSingle() { return null; }",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getSingle();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx2Completable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import io.reactivex.Completable;",
            "class Test {",
            "  Completable getCompletable() { return null; } ",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getCompletable();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx2Flowable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import io.reactivex.Flowable;",
            "class Test {",
            "  Flowable getFlowable() { return null; } ",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getFlowable();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx2Maybe() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import io.reactivex.Maybe;",
            "class Test {",
            "  Maybe getMaybe() { return null; }",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getMaybe();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx1Observable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import rx.Observable;",
            "class Test {",
            "  Observable getObservable() { return null; }",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getObservable();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx1Single() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import rx.Single;",
            "class Test {",
            "  Single getSingle() { return null; }",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getSingle();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rx1Completable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import rx.Completable;",
            "class Test {",
            "  Completable getCompletable() { return null; } ",
            "  void f() {",
            "    // BUG: Diagnostic contains: Rx objects must be checked.",
            "    getCompletable();",
            "  }",
            "}")
        .doTest();
  }
}
