/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

/**
 * @author gak@google.com (Gregory Kick)
 */
@RunWith(JUnit4.class)
public class RemoveUnusedImportsTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(RemoveUnusedImports.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("RemoveUnusedImportsPositiveCases.java").doTest();
  }

  @Test
  public void useInSelect() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new RemoveUnusedImports(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.util.Map;",
            "import java.util.Map.Entry;",
            "public class Test {",
            "  Map.Entry<String, String> e;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Map;",
            "public class Test {",
            "  Map.Entry<String, String> e;",
            "}")
        .doTest();
  }
}
