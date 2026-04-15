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

/** Tests for {@link ThrowableEqualsHashCode}. */
@RunWith(JUnit4.class)
public final class ThrowableEqualsHashCodeTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ThrowableEqualsHashCode.class, getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
              @Override
              public boolean equals(Object obj) {
                return false;
              }

              @Override
              public int hashCode() {
                return 42;
              }
            }
            """)
        .addOutputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
            }
            """)
        .doTest();
  }

  @Test
  public void positiveEquals() {
    refactoringHelper
        .addInputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
              @Override
              public boolean equals(Object obj) {
                return false;
              }
            }
            """)
        .addOutputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
            }
            """)
        .doTest();
  }

  @Test
  public void positiveHashCode() {
    refactoringHelper
        .addInputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
              @Override
              public int hashCode() {
                return 42;
              }
            }
            """)
        .addOutputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
            }
            """)
        .doTest();
  }

  @Test
  public void negativeIntegerWrapper() {
    refactoringHelper
        .addInputLines(
            "IntegerWrapper.java",
            """
            public final class IntegerWrapper {
              private final int value;

              public IntegerWrapper(int value) {
                this.value = value;
              }

              @Override
              public boolean equals(Object object) {
                return object instanceof IntegerWrapper that && this.value == that.value;
              }

              @Override
              public int hashCode() {
                return value;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeThrowableNoOverrides() {
    refactoringHelper
        .addInputLines(
            "TestException.java",
            """
            public class TestException extends Exception {
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
