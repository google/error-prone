/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.exceptions.base.MockitoException;

/**
 * Tests for {@code CannotMockFinalClass}.
 *
 * @author Louis Wasserman
 */
@RunWith(JUnit4.class)
public class CannotMockFinalClassTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(CannotMockFinalClass.class, getClass());
  }

  static final class FinalClass {
  }

  static class MocksFinalClassWithAnnotation {
    @Mock
    FinalClass impossible;
  }

  @Test
  public void mockingFinalClassWithAnnotationFails() {
    exception.expect(MockitoException.class);
    MockitoAnnotations.initMocks(new MocksFinalClassWithAnnotation());
  }

  @Test
  public void mockingFinalClassWithMockMethodFails() {
    exception.expect(MockitoException.class);
    Mockito.mock(FinalClass.class);
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("CannotMockFinalClassPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("CannotMockFinalClassNegativeCases.java").doTest();
  }

  @Test
  public void testNegativeCase2() throws Exception {
    compilationHelper.addSourceFile("CannotMockFinalClassNegativeCases2.java").doTest();
  }

}
