/*
 * Copyright 2018 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ParcelableCreator}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ParcelableCreatorTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ParcelableCreator.class, getClass())
          .addSourceFile("testdata/stubs/android/os/Parcel.java")
          .addSourceFile("testdata/stubs/android/os/Parcelable.java");

  @Test
  public void positive() {
    compilationHelper.addSourceFile("ParcelableCreatorPositiveCases.java").doTest();
  }

  @Test
  public void negative() {
    compilationHelper.addSourceFile("ParcelableCreatorNegativeCases.java").doTest();
  }
}
