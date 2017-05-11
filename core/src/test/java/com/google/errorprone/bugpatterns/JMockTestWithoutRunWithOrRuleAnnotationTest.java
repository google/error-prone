/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JMockTestWithoutRunWithOrRuleAnnotationTest {

  private CompilationTestHelper compilationTestHelper;

  @Before
  public void setup() {
    compilationTestHelper =
        CompilationTestHelper.newInstance(
            JMockTestWithoutRunWithOrRuleAnnotation.class, getClass());
  }

  @Test
  public void testShouldFlagNoRuleAndNoRunWith() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.Mockery;",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  private final Mockery mockery = new Mockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldFlagWrongRunWith() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.Mockery;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.Parameterized;",
            "@RunWith(Parameterized.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  private final Mockery mockery = new Mockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldFlagJUnitRuleMockery() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.integration.junit4.JUnitRuleMockery;",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public final JUnitRuleMockery mockery = new JUnitRuleMockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldNotFlagWithRuleAnnotation() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.Mockery;",
            "import org.jmock.integration.junit4.JUnitRuleMockery;",
            "import org.junit.Rule;",
            "public class Test {",
            "  @Rule",
            "  private final Mockery mockery = new Mockery();",
            "  @Rule",
            "  public final JUnitRuleMockery mockery2 = new JUnitRuleMockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldNotFlagWithRuleAnnotationAndWrongRunWith() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.Mockery;",
            "import org.jmock.integration.junit4.JUnitRuleMockery;",
            "import org.junit.Rule;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.Parameterized;",
            "@RunWith(Parameterized.class)",
            "public class Test {",
            "  @Rule",
            "  private final Mockery mockery = new Mockery();",
            "  @Rule",
            "  public final JUnitRuleMockery mockery2 = new JUnitRuleMockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldNotFlagRunWithJMock() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.Mockery;",
            "import org.jmock.integration.junit4.JMock;",
            "import org.jmock.integration.junit4.JUnitRuleMockery;",
            "import org.junit.runner.RunWith;",
            "@RunWith(JMock.class)",
            "public class Test {",
            "  private final Mockery mockery = new org.jmock.Mockery();",
            "  public final JUnitRuleMockery mockery2 = new JUnitRuleMockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldNotFlagWithRuleAnnotationAndRunWithJMock() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jmock.Mockery;",
            "import org.jmock.integration.junit4.JMock;",
            "import org.jmock.integration.junit4.JUnitRuleMockery;",
            "import org.junit.Rule;",
            "import org.junit.runner.RunWith;",
            "@RunWith(JMock.class)",
            "public class Test {",
            "  @Rule",
            "  private final Mockery mockery = new Mockery();",
            "  @Rule",
            "  public final JUnitRuleMockery mockery2 = new JUnitRuleMockery();",
            "}")
        .doTest();
  }

  @Test
  public void testShouldNotFlagSingleFieldWithRuleAnnotation() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.Rule;",
            "import org.jmock.Mockery;",
            "public class Test {",
            "  @Rule",
            "  final Mockery mockery = new Mockery();",
            "}")
        .doTest();
  }
}
