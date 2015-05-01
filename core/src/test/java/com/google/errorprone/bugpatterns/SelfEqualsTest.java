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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.fail;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class SelfEqualsTest {
  CompilationTestHelper compilationHelper;

  @BugPattern(
      name = "SelfEquals", summary = "An object is tested for equality to itself", category = GUAVA,
      severity = ERROR, maturity = MATURE)
  public static class SelfEqualsTestChecker extends SelfEquals {
    public SelfEqualsTestChecker() {
      super(true, true);
    }
  }

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(SelfEqualsTestChecker.class, getClass());
  }

  @Test
  public void testPositiveCase1() throws Exception {
    compilationHelper.addSourceFile("SelfEqualsPositiveCase1.java").doTest();
  }

  @Test
  public void testPositiveCase2() throws Exception {
    compilationHelper.addSourceFile("SelfEqualsPositiveCase2.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("SelfEqualsNegativeCases.java").doTest();
  }

  @BugPattern(
      name = "SelfEquals", summary = "An object is tested for equality to itself", category = GUAVA,
      severity = ERROR, maturity = MATURE)
  public static class SelfEquals_Guava_Equals extends SelfEquals {
    public SelfEquals_Guava_Equals() {
      super(true, true);
    }
  }

  @BugPattern(
      name = "SelfEquals", summary = "An object is tested for equality to itself", category = GUAVA,
      severity = ERROR, maturity = MATURE)
  public static class SelfEquals_Guava extends SelfEquals {
    public SelfEquals_Guava() {
      super(true, false);
    }
  }

  @BugPattern(
      name = "SelfEquals", summary = "An object is tested for equality to itself", category = GUAVA,
      severity = ERROR, maturity = MATURE)
  public static class SelfEquals_Equals extends SelfEquals {
    public SelfEquals_Equals() {
      super(false, true);
    }
  }

  @Test
  public void testFlags() throws Exception {
    Class<? extends SelfEquals> checker;
    CompilationTestHelper compilationHelper;
    // Both checks off.
    try {
      new SelfEquals(false, false);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected to get an exception.
    }

    // Both checks on.
    checker = SelfEquals_Guava_Equals.class;
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
    compilationHelper.addSourceFile("SelfEqualsPositiveCase1.java").doTest();
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
    compilationHelper.addSourceFile("SelfEqualsPositiveCase2.java").doTest();

    // Guava on, Equals off.
    checker = SelfEquals_Guava.class;
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
    compilationHelper.addSourceFile("SelfEqualsPositiveCase1.java").doTest();
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
    compilationHelper.addSourceFile("SelfEqualsPositiveCase2.java")
        .expectNoDiagnostics()
        .doTest();

    // Equals on, Guava off.
    checker = SelfEquals_Equals.class;
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
    compilationHelper.addSourceFile("SelfEqualsPositiveCase1.java")
        .expectNoDiagnostics()
        .doTest();
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
    compilationHelper.addSourceFile("SelfEqualsPositiveCase2.java").doTest();
  }

}
