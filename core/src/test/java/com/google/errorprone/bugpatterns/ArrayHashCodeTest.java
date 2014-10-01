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

import static com.google.common.base.StandardSystemProperty.JAVA_VERSION;
import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class ArrayHashCodeTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new ArrayHashCode());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "ArrayHashCodePositiveCases.java"));
  }

  /**
   * Tests java.util.Objects hashCode methods, which are only in JDK 7 and above.
   */
  @Test
  public void testJava7PositiveCase() throws Exception {
    String[] javaVersion = JAVA_VERSION.value().split("\\.");
    assumeTrue(Integer.parseInt(javaVersion[1]) >= 7);
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "ArrayHashCodePositiveCases2.java"));
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "ArrayHashCodeNegativeCases.java"));
  }

  /**
   * Tests java.util.Objects hashCode methods, which are only in JDK 7 and above.
   */
  @Test
  public void testJava7NegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "ArrayHashCodeNegativeCases2.java"));
  }
}
