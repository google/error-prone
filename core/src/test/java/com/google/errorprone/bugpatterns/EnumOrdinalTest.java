/*
 * Copyright 2024 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class EnumOrdinalTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(EnumOrdinal.class, getClass());

  @Test
  public void positive_enumOrdinal() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            enum TestEnum {
              FOO,
              BAR,
            }

            class Caller {
              public int callOrdinal() {
                // BUG: Diagnostic contains: ordinal
                return TestEnum.FOO.ordinal();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_enumValues_externalCall() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            enum TestEnum {
              FOO,
              BAR,
            }

            class Caller {
              public TestEnum callValues() {
                // BUG: Diagnostic contains: ordinal
                return TestEnum.values()[0];
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_enumValues_internalCall() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            enum TestEnum {
              FOO,
              BAR;

              private static TestEnum fromValues() {
                return values()[0];
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_enumValues_noIndex() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            enum TestEnum {
              FOO,
              BAR,
            }

            class Caller {
              public TestEnum[] callValues() {
                return TestEnum.values();
              }
            }
            """)
        .doTest();
  }
}
