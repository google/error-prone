/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AndroidInjectionBeforeSuper}. */
@RunWith(JUnit4.class)
public final class AndroidInjectionBeforeSuperTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(AndroidInjectionBeforeSuper.class, getClass())
            .addSourceFile("testdata/stubs/android/app/Activity.java")
            .addSourceFile("testdata/stubs/android/app/Fragment.java")
            .addSourceFile("testdata/stubs/android/app/Service.java")
            .addSourceFile("testdata/stubs/android/content/Context.java")
            .addSourceFile("testdata/stubs/android/content/Intent.java")
            .addSourceFile("testdata/stubs/android/os/Bundle.java")
            .addSourceFile("testdata/stubs/android/os/IBinder.java");
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceFile("AndroidInjectionBeforeSuperPositiveCases.java")
        .addSourceFile("AndroidInjection.java")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceFile("AndroidInjectionBeforeSuperNegativeCases.java")
        .addSourceFile("AndroidInjection.java")
        .doTest();
  }
}
