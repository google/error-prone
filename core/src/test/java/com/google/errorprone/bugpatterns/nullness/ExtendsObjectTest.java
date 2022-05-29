/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ExtendsObject}. */
@RunWith(JUnit4.class)
public final class ExtendsObjectTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(ExtendsObject.class, getClass());

  @Test
  public void positive() {
    helper
        .addInputLines(
            "Test.java", //
            "class Foo<T extends Object> {}")
        .addOutputLines(
            "Test.java", //
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "class Foo<T extends @NonNull Object> {}")
        .doTest();
  }

  @Test
  public void extendsParameterWithObjectErasure_noFinding() {
    helper
        .addInputLines(
            "Test.java", //
            "class Foo<S, T extends S> {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addInputLines(
            "Test.java", //
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "class Foo<T extends @NonNull Object> {}")
        .expectUnchanged()
        .doTest();
  }
}
