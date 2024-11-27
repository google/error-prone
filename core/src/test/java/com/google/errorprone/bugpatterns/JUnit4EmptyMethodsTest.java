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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JUnit4EmptyMethods}. */
@RunWith(JUnit4.class)
public class JUnit4EmptyMethodsTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnit4EmptyMethods.class, getClass());

  @Test
  public void emptyMethods() {
    refactoringHelper
        .addInputLines(
            "FooTest.java",
            """
            import org.junit.After;
            import org.junit.AfterClass;
            import org.junit.Before;
            import org.junit.BeforeClass;
            import org.junit.Test;

            class FooTest {
              @Before
              public void setUp() {}

              @BeforeClass
              public void setUpClass() {}

              @After
              public void after() {}

              @AfterClass
              public void afterClass() {}

              @Test
              public void emptyTest() {}
            }
            """)
        .addOutputLines(
            "FooTest.java",
            """
            import org.junit.After;
            import org.junit.AfterClass;
            import org.junit.Before;
            import org.junit.BeforeClass;
            import org.junit.Test;
            class FooTest {
              @Test
              public void emptyTest() {}
            }
            """)
        .doTest();
  }

  @Test
  public void emptyMethodsWithComments() {
    refactoringHelper
        .addInputLines(
            "FooTest.java",
            """
            import org.junit.After;
            import org.junit.AfterClass;
            import org.junit.Before;
            import org.junit.BeforeClass;
            import org.junit.Test;

            class FooTest {
              @Before
              public void setUp() {
                // comments are ignored
              }

              @BeforeClass
              public void setUpClass() {
                // comments are ignored
              }

              @After
              public void after() {
                // comments are ignored
              }

              @AfterClass
              public void afterClass() {
                // comments are ignored
              }

              @Test
              public void emptyTest() {
                // comments are ignored
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nonEmptyMethods() {
    refactoringHelper
        .addInputLines(
            "FooTest.java",
            """
            import static org.junit.Assert.assertEquals;

            import org.junit.After;
            import org.junit.AfterClass;
            import org.junit.Before;
            import org.junit.BeforeClass;
            import org.junit.Test;

            class FooTest {
              @Before
              public void setUp() {
                System.out.println("setUp()");
              }

              @BeforeClass
              public void setUpClass() {
                System.out.println("setUpClass()");
              }

              @After
              public void after() {
                System.out.println("after()");
              }

              @AfterClass
              public void afterClass() {
                System.out.println("afterClass()");
              }

              @Test
              public void nonEmptyTest() {
                assertEquals(1, 1);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void abstractMethods() {
    refactoringHelper
        .addInputLines(
            "FooTest.java",
            """
            import org.junit.After;
            import org.junit.AfterClass;
            import org.junit.Before;
            import org.junit.BeforeClass;
            import org.junit.Test;

            abstract class FooTest {
              @Before
              public abstract void setUp();

              @BeforeClass
              public abstract void setUpClass();

              @After
              public abstract void after();

              @AfterClass
              public abstract void afterClass();

              @Test
              public abstract void emptyTest();
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void overriddenMethod() {
    refactoringHelper
        .addInputLines(
            "FooBase.java",
            """
            class FooBase {
              public void setUp() {
                System.out.println("parent setUp()");
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "FooTest.java",
            """
            import org.junit.Before;

            class FooTest extends FooBase {
              @Before
              public void setUp() {
                // don't delete this because it's an override!
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
