/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link UrlInSee}. */
@RunWith(JUnit4.class)
public final class UrlInSeeTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(UrlInSee.class, getClass());

  @Test
  public void positive() {
    helper
        .addInputLines(
            "Test.java", //
            "/**",
            " * @see http://foo for more details",
            "*/",
            "class Test {}")
        .addOutputLines(
            "Test.java", //
            "/**",
            " * See http://foo for more details",
            "*/",
            "class Test {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    helper
        .addInputLines(
            "Test.java", //
            "/**",
            " * @see java.util.List",
            "*/",
            "class Test {}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
