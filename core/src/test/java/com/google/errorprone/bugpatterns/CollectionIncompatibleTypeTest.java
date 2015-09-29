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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class CollectionIncompatibleTypeTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(CollectionIncompatibleType.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeNegativeCases.java").doTest();
  }

  @Test
  public void testOutOfBounds() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeOutOfBounds.java").doTest();
  }

  @Test
  public void testClassCast() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeClassCast.java").doTest();
  }

  // This test is disabled because calling Types#asSuper in the check removes the upper bound on K.
  @Test
  @Ignore
  public void testBoundedTypeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.HashMap;",
            "public class Test {",
            "  private static class MyHashMap<K extends Integer, V extends String>",
            "      extends HashMap<K, V> {}",
            "  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {",
            "    // BUG: Diagnostic contains:",
            "    return myHashMap.containsKey(\"bad\");",
            "  }",
            "}")
        .doTest();
  }
}
