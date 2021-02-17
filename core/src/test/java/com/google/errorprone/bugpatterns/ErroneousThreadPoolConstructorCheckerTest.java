/*
 * Copyright 2021 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ErroneousThreadPoolConstructorChecker} bug pattern. */
@RunWith(JUnit4.class)
public class ErroneousThreadPoolConstructorCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ErroneousThreadPoolConstructorChecker.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          new ErroneousThreadPoolConstructorChecker(), getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceFile("ErroneousThreadPoolConstructorCheckerPositiveCases.java")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceFile("ErroneousThreadPoolConstructorCheckerNegativeCases.java")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_literalConstantsForPoolSize_refactorUsingFirstFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.FIRST)
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_corePoolSizeZero_refactorUsingFirstFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.FIRST)
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(0, 20, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_literalConstantsForPoolSize_refactorUsingSecondFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.SECOND)
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_staticConstantsForPoolSize_refactorUsingFirstFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.FIRST)
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  private static final int CORE_SIZE = 10;",
            "  private static final int MAX_SIZE = 20;",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  private static final int CORE_SIZE = 10;",
            "  private static final int MAX_SIZE = 20;",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(CORE_SIZE, CORE_SIZE, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_staticConstantsForPoolSize_refactorUsingSecondFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.SECOND)
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  private static final int CORE_SIZE = 10;",
            "  private static final int MAX_SIZE = 20;",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.LinkedBlockingQueue;",
            "import java.util.concurrent.ThreadPoolExecutor;",
            "import java.util.concurrent.TimeUnit;",
            "",
            "class Test {",
            "  private static final int CORE_SIZE = 10;",
            "  private static final int MAX_SIZE = 20;",
            "  public void createThreadPool() {",
            "    new ThreadPoolExecutor(MAX_SIZE, MAX_SIZE, 60, TimeUnit.SECONDS,",
            "                           new LinkedBlockingQueue<>());",
            "  }",
            "}")
        .doTest();
  }
}
