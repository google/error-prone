/*
 * Copyright 2026 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class UnnecessarySemicolonTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessarySemicolon.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private final Runnable r = () -> {};
              ;

              void f() {
                System.err.println();
                ;
              }
              ;
            }
            ;
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private final Runnable r = () -> {};

              void f() {
                System.err.println();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveSwitch() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f(int i) {
                switch (i) {
                  case 1:
                    ;
                    break;
                  case 2:
                    break;
                  default:
                    ;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f(int i) {
                switch (i) {
                  case 1:
                    break;
                  case 2:
                    break;
                  default:
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void emptyBlock() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (; ; ) {}
              }

              void g() {
                while (true)
                  ;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (; ; ) {}
              }

              void g() {
                while (true) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeEnum() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum E {
                ONE,
                TWO;
              }

              enum F {
                ONE,
                TWO;
                int x;
              }

              enum G {
                ONE
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void multiVariable() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                int x, y;
                ;
                int z;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                int x, y;
                int z;
              }
            }
            """)
        .doTest();
  }
}
