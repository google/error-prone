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
