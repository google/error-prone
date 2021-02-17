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
