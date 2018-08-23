/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author anishvisaria98@gmail.com (Anish Visaria) */
@RunWith(JUnit4.class)
public class ModifyCollectionInEnhancedForLoopTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(ModifyCollectionInEnhancedForLoop.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("ModifyCollectionInEnhancedForLoopPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ModifyCollectionInEnhancedForLoopNegativeCases.java").doTest();
  }

  @Test
  public void modifyCollectionInItself() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.AbstractCollection;",
            "import java.util.Collection;",
            "abstract class Test<E> extends AbstractCollection<E> {",
            "  public boolean addAll(Collection<? extends E> c) {",
            "    boolean modified = false;",
            "    for (E e : c)",
            "      if (add(e))",
            "        modified = true;",
            "    return modified;",
            "  }",
            "}")
        .doTest();
  }
}
