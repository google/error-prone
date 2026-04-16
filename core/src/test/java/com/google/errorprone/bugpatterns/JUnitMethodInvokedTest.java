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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link JUnitMethodInvoked}. */
@RunWith(JUnit4.class)
public class JUnitMethodInvokedTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnitMethodInvoked.class, getClass());

  @Test
  public void positiveCaseJUnit4() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import org.junit.Test;

            class MyTest {
              @Test
              public void testFoo() {}

              public void callTest() {
                // BUG: Diagnostic contains: JUnitMethodInvoked
                testFoo();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCaseJUnit3() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              public void testFoo() {}

              public void callTest() {
                // BUG: Diagnostic contains: JUnitMethodInvoked
                testFoo();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_setUp() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              public void setUp() {}

              public void callSetUp() {
                setUp();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_tearDown() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              public void tearDown() {}

              public void callTearDown() {
                tearDown();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit4_before() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import org.junit.Before;

            class MyTest {
              @Before
              public void setUp() {}

              public void callSetUp() {
                setUp();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit4_after() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import org.junit.After;

            class MyTest {
              @After
              public void tearDown() {}

              public void callTearDown() {
                tearDown();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit4_beforeClass() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import org.junit.BeforeClass;

            class MyTest {
              @BeforeClass
              public static void setUpClass() {}

              public void callSetUpClass() {
                setUpClass();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit4_afterClass() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import org.junit.AfterClass;

            class MyTest {
              @AfterClass
              public static void tearDownClass() {}

              public void callTearDownClass() {
                tearDownClass();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_notTestCase() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            class MyTest {
              public void testFoo() {}

              public void callTest() {
                testFoo();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_notPublic() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              void testFoo() {}

              public void callTest() {
                testFoo();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_hasParams() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              public void testFoo(int x) {}

              public void callTest() {
                testFoo(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_notVoid() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              public int testFoo() {
                return 0;
              }

              public void callTest() {
                var unused = testFoo();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_super_direct() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class MyTest extends TestCase {
              @Override
              public void setUp() throws Exception {
                super.setUp();
              }

              @Override
              public void tearDown() throws Exception {
                super.tearDown();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseJUnit3_super_indirect() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class BaseTest extends TestCase {
              @Override
              public void setUp() throws Exception {
                super.setUp();
              }

              @Override
              public void tearDown() throws Exception {
                super.tearDown();
              }
            }

            class MyTest extends BaseTest {
              @Override
              public void setUp() throws Exception {
                super.setUp();
              }

              @Override
              public void tearDown() throws Exception {
                super.tearDown();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCaseJUnit3_superTestMethod() {
    compilationHelper
        .addSourceLines(
            "MyTest.java",
            """
            import junit.framework.TestCase;

            class BaseTest extends TestCase {
              public void testFoo() {}
            }

            class MyTest extends BaseTest {
              public void testBar() {
                // BUG: Diagnostic contains: JUnitMethodInvoked
                super.testFoo();
              }
            }
            """)
        .doTest();
  }
}
