/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.android;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test cases for Android's @RestrictTo annotation. */
@RunWith(JUnit4.class)
public final class RestrictToEnforcerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(RestrictToEnforcer.class, getClass());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper
        .addSourceFile("stubs/android/support/annotation/RestrictTo.java")
        .addSourceFile("RestrictToEnforcerPositiveCases.java")
        .addSourceFile("RestrictToEnforcerPositiveCasesApi.java")
        .doTest();
  }
}
