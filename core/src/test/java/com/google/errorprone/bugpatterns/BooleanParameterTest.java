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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link BooleanParameter}Test */
@RunWith(JUnit4.class)
public class BooleanParameterTest {

  @Test
  public void refactoring() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new BooleanParameter(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(boolean foo) {}",
            "  void f(boolean foo, boolean bar) {}",
            "  void g(boolean p, boolean q) {}",
            "  void h(boolean arg0, boolean arg1) {}",
            "  {",
            "    f(/* foo= */ true);",
            "    f(false); // one arg",
            "    f(/* foo= */ true, false);",
            "    f(false, false);",
            "    g(false, false); // single-char",
            "    h(false, false); // synthetic",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(boolean foo) {}",
            "  void f(boolean foo, boolean bar) {}",
            "  void g(boolean p, boolean q) {}",
            "  void h(boolean arg0, boolean arg1) {}",
            "  {",
            "    f(/* foo= */ true);",
            "    f(false); // one arg",
            "    f(/* foo= */ true, /* bar= */ false);",
            "    f(/* foo= */ false, /* bar= */ false);",
            "    g(false, false); // single-char",
            "    h(false, false); // synthetic",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }
}
