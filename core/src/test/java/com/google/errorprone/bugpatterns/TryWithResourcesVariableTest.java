/*
 * Copyright 2021 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link TryWithResourcesVariable}Test */
@RunWith(JUnit4.class)
public class TryWithResourcesVariableTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(TryWithResourcesVariable.class, getClass());

  @Test
  public void refactoring() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f (AutoCloseable r1) {",
            "    try (AutoCloseable r2 = r1) {",
            "      System.err.println(r2);",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f (AutoCloseable r1) {",
            "    try (r1) {",
            "      System.err.println(r1);",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTwoVariables() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f (AutoCloseable a1, AutoCloseable a2) {",
            "    try (AutoCloseable b1 = a1; AutoCloseable b2 = a2) {",
            "      System.err.println(b1);",
            "      System.err.println(b2);",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f (AutoCloseable a1, AutoCloseable a2) {",
            "    try (a1; a2) {",
            "      System.err.println(a1);",
            "      System.err.println(a2);",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNonFinal() {
    testHelper
        .addInputLines(
            "Test.java",
            "abstract class Test {",
            "  abstract AutoCloseable reassign(AutoCloseable r);",
            "  void f (AutoCloseable r1) {",
            "    r1 = reassign(r1);",
            "    try (AutoCloseable r2 = r1) {",
            "      System.err.println(r2);",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
