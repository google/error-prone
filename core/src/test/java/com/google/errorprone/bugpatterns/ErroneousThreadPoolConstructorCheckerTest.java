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
          ErroneousThreadPoolConstructorChecker.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "ErroneousThreadPoolConstructorCheckerPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import static java.util.Comparator.comparingInt;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Positive test cases for {@link
 * com.google.errorprone.bugpatterns.ErroneousThreadPoolConstructorChecker} bug pattern.
 */
final class ErroneousThreadPoolConstructorCheckerPositiveCases {

  private static final int CORE_POOL_SIZE = 10;
  private static final int MAXIMUM_POOL_SIZE = 20;
  private static final long KEEP_ALIVE_TIME = 60;

  private void createThreadPoolWithUnboundedLinkedBlockingQueue(Collection<Runnable> initialTasks) {
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, new LinkedBlockingQueue<>());
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new LinkedBlockingQueue<>(initialTasks));
  }

  private void createThreadPoolWithUnboundedLinkedBlockingDeque(Collection<Runnable> initialTasks) {
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, new LinkedBlockingDeque<>());
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new LinkedBlockingDeque<>(initialTasks));
  }

  private void createThreadPoolWithUnboundedLinkedTransferQueue(Collection<Runnable> initialTasks) {
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, new LinkedTransferQueue<>());
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new LinkedTransferQueue<>(initialTasks));
  }

  private void createThreadPoolWithUnboundedPriorityBlockingQueue(
      int initialCapacity, Collection<Runnable> initialTasks) {
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, new PriorityBlockingQueue<>());
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new PriorityBlockingQueue<>(initialTasks));
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new PriorityBlockingQueue<>(initialCapacity));
    // BUG: Diagnostic contains: ErroneousThreadPoolConstructorChecker
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new PriorityBlockingQueue<>(initialCapacity, comparingInt(Object::hashCode)));
  }
}
""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "ErroneousThreadPoolConstructorCheckerNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Negative test cases for {@link
 * com.google.errorprone.bugpatterns.ErroneousThreadPoolConstructorChecker} bug pattern.
 */
final class ErroneousThreadPoolConstructorCheckerNegativeCases {

  private static final int CORE_POOL_SIZE = 10;
  private static final int MAXIMUM_POOL_SIZE = 20;
  private static final long KEEP_ALIVE_TIME = 60;

  private void createThreadPoolWithUnboundedQueue() {
    new ThreadPoolExecutor(
        MAXIMUM_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new LinkedBlockingQueue<>());
  }

  private void createThreadPoolWithUnboundedQueueAndEmptyPool() {
    new ThreadPoolExecutor(0, 1, KEEP_ALIVE_TIME, SECONDS, new LinkedBlockingQueue<>());
  }

  private void createThreadPoolWithBoundedArrayBlockingQueue(
      int initialCapacity, boolean fair, Collection<Runnable> initialTasks) {
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new ArrayBlockingQueue<>(initialCapacity));
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new ArrayBlockingQueue<>(initialCapacity, fair));
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new ArrayBlockingQueue<>(initialCapacity, fair, initialTasks));
  }

  private void createThreadPoolWithBoundedLinkedBlockingQueue(int capacity) {
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new LinkedBlockingQueue<>(capacity));
  }

  private void createThreadPoolWithBoundedLinkedBlockingDeque(int capacity) {
    new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        SECONDS,
        new LinkedBlockingDeque<>(capacity));
  }

  private void createThreadPoolWithBoundedSynchronousQueue() {
    new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, new SynchronousQueue<>());
  }
}
""")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_literalConstantsForPoolSize_refactorUsingFirstFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.FIRST)
        .addInputLines(
            "Test.java",
            """
            import java.util.concurrent.LinkedBlockingQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;

            class Test {
              public void createThreadPool() {
                new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.concurrent.LinkedBlockingQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;

            class Test {
              public void createThreadPool() {
                new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_corePoolSizeZero_refactorUsingFirstFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.FIRST)
        .addInputLines(
            "Test.java",
            """
            import java.util.concurrent.LinkedBlockingQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;

            class Test {
              public void createThreadPool() {
                new ThreadPoolExecutor(0, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.concurrent.LinkedBlockingQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;

            class Test {
              public void createThreadPool() {
                new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_literalConstantsForPoolSize_refactorUsingSecondFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.SECOND)
        .addInputLines(
            "Test.java",
            """
            import java.util.concurrent.LinkedBlockingQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;

            class Test {
              public void createThreadPool() {
                new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.concurrent.LinkedBlockingQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;

            class Test {
              public void createThreadPool() {
                new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_staticConstantsForPoolSize_refactorUsingFirstFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.FIRST)
        .addInputLines(
            "Test.java",
"""
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class Test {
  private static final int CORE_SIZE = 10;
  private static final int MAX_SIZE = 20;

  public void createThreadPool() {
    new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
  }
}
""")
        .addOutputLines(
            "Test.java",
"""
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class Test {
  private static final int CORE_SIZE = 10;
  private static final int MAX_SIZE = 20;

  public void createThreadPool() {
    new ThreadPoolExecutor(CORE_SIZE, CORE_SIZE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
  }
}
""")
        .doTest();
  }

  @Test
  public void erroneousThreadPoolConstructor_staticConstantsForPoolSize_refactorUsingSecondFix() {
    refactoringHelper
        .setFixChooser(FixChoosers.SECOND)
        .addInputLines(
            "Test.java",
"""
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class Test {
  private static final int CORE_SIZE = 10;
  private static final int MAX_SIZE = 20;

  public void createThreadPool() {
    new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
  }
}
""")
        .addOutputLines(
            "Test.java",
"""
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class Test {
  private static final int CORE_SIZE = 10;
  private static final int MAX_SIZE = 20;

  public void createThreadPool() {
    new ThreadPoolExecutor(MAX_SIZE, MAX_SIZE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
  }
}
""")
        .doTest();
  }
}
