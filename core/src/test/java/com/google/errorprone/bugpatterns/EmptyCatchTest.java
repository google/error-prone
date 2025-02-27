/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

/**
 * @author yuan@ece.toronto.edu (Ding Yuan)
 */
@RunWith(JUnit4.class)
public class EmptyCatchTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(EmptyCatch.class, getClass());

  @Test
  public void positiveCase() throws Exception {
    compilationHelper
        .addSourceLines(
            "EmptyCatchPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns;

            import static org.junit.Assert.fail;

            import org.junit.Test;

            /**
             * @author yuan@ece.toronto.edu (Ding Yuan)
             */
            public class EmptyCatchPositiveCases {
              public void error() throws IllegalArgumentException {
                throw new IllegalArgumentException("Fake exception.");
              }

              public void catchIsCompleteEmpty() {
                try {
                  error();
                } // BUG: Diagnostic contains:
                catch (Throwable t) {

                }
              }

              @Test
              public void expectedException() {
                try {
                  System.err.println();
                  fail();
                  // BUG: Diagnostic contains:
                } catch (Exception expected) {
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() throws Exception {
    compilationHelper
        .addSourceLines(
            "EmptyCatchNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns;


            import java.io.FileNotFoundException;

            /**
             * @author yuan@ece.toronto.edu (Ding Yuan)
             */
            public class EmptyCatchNegativeCases {
              public void error() throws IllegalArgumentException {
                throw new IllegalArgumentException("Fake exception.");
              }

              public void harmlessError() throws FileNotFoundException {
                throw new FileNotFoundException("harmless exception.");
              }

              public void close() throws IllegalArgumentException {
                // close() is an allowed method, so any exceptions
                // thrown by this method can be ignored!
                throw new IllegalArgumentException("Fake exception.");
              }

              public void handledException() {
                int a = 0;
                try {
                  error();
                } catch (Exception e) {
                  a++; // handled here
                }
              }

              public void exceptionHandledByDataflow() {
                int a = 0;
                try {
                  error();
                  a = 10;
                } catch (Throwable t) {
                  /* Although the exception is ignored here, it is actually
                   * handled by the if check below.
                   */
                }
                if (a != 10) {
                  System.out.println("Exception is handled here..");
                  a++;
                }
              }

              public void exceptionHandledByControlFlow() {
                try {
                  error();
                  return;
                } catch (Throwable t) {
                  /* Although the exception is ignored here, it is actually
                   * handled by the return statement in the try block.
                   */
                }
                System.out.println("Exception is handled here..");
              }

              public void alreadyInCatch() {
                try {
                  error();
                } catch (Throwable t) {
                  try {
                    error();
                  } catch (Exception e) {
                    // Although e is ignored, it is OK b/c we're already
                    // in a nested catch block.
                  }
                }
              }

              public void harmlessException() {
                try {
                  harmlessError();
                } catch (FileNotFoundException e) {
                  /* FileNotFoundException is a harmless exception and
                   * it is OK to ignore it.
                   */
                }
              }

              public void exemptedMethod() {
                try {
                  close();
                } catch (Exception e) {
                  // Although the exception is ignored, we can allow this b/c
                  // it is thrown by an exempted method.
                }
              }

              public void comment() {
                int a = 0; // TODO
                try {
                  error();
                  // TODO
                  /* FIXME */
                } catch (Throwable t) {
                  // ignored
                }
              }

              public void catchIsLoggedOnly() {
                try {
                  error();
                } catch (Throwable t) {
                  System.out.println("Caught an exception: " + t);
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void addTestNgTest() {
    compilationHelper
        .addSourceLines(
            "org/testng/annotations/Test.java",
            """
            package org.testng.annotations;

            public @interface Test {}
            """)
        .addSourceLines(
            "in/SomeTest.java",
            """
            import org.testng.annotations.Test;

            public class SomeTest {
              @Test
              public void testNG() {
                try {
                  System.err.println();
                // BUG: Diagnostic contains:
                } catch (Exception doNotCare) {
                }
              }
            }
            """)
        .doTest();
  }
}
