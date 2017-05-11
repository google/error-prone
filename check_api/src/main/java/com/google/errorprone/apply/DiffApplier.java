/*
 * Copyright 2011 Google Inc. All rights reserved.
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

package com.google.errorprone.apply;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applier of diffs to Java source code
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class DiffApplier extends AbstractService {
  private static final Logger logger = Logger.getLogger(DiffApplier.class.getName());
  private final ExecutorService workerService;
  private final Set<String> refactoredPaths;
  private final Set<String> diffsFailedPaths;
  private final FileSource source;
  private final FileDestination destination;
  private final AtomicInteger completedFiles;
  private final Stopwatch stopwatch;

  // the number of diffs in flight, plus 1 if the service is in the RUNNING state
  private final AtomicInteger runState = new AtomicInteger();

  public DiffApplier(int diffParallelism, FileSource source, FileDestination destination) {
    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(destination);
    this.diffsFailedPaths = new ConcurrentSkipListSet<>();
    this.refactoredPaths = Sets.newConcurrentHashSet();
    this.source = source;
    this.destination = destination;
    this.completedFiles = new AtomicInteger(0);
    this.stopwatch = Stopwatch.createUnstarted();
    // configure a bounded queue and a rejectedexecutionpolicy.
    // In this case CallerRuns may be appropriate.
    this.workerService =
        new ThreadPoolExecutor(
            0,
            diffParallelism,
            5,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(50),
            new ThreadPoolExecutor.CallerRunsPolicy());
  }

  @Override
  protected void doStart() {
    stopwatch.start();
    runState.incrementAndGet();
    notifyStarted();
  }

  @Override
  protected void doStop() {
    decrementTasks(); // matches the increment in doStart();
  }

  private final void decrementTasks() {
    if (runState.decrementAndGet() == 0) {
      workerService.shutdown();
      try {
        destination.flush();
        notifyStopped();
      } catch (Exception e) {
        notifyFailed(e);
      }
      logger.log(
          Level.INFO, String.format("Completed %d files in %s", completedFiles.get(), stopwatch));
      if (!diffsFailedPaths.isEmpty()) {
        logger.log(
            Level.SEVERE,
            String.format(
                "Diffs failed to apply to %d files: %s",
                diffsFailedPaths.size(), Iterables.limit(diffsFailedPaths, 30)));
      }
    }
  }

  private final class Task implements Runnable {
    private final Diff diff;

    Task(Diff diff) {
      this.diff = diff;
    }

    @Override
    public void run() {
      try {
        SourceFile file = source.readFile(diff.getRelevantFileName());
        diff.applyDifferences(file);
        destination.writeFile(file);

        int completed = completedFiles.incrementAndGet();
        if (completed % 100 == 0) {
          logger.log(Level.INFO, String.format("Completed %d files in %s", completed, stopwatch));
        }
      } catch (IOException | DiffNotApplicableException e) {
        logger.log(Level.WARNING, "Failed to apply diff to file " + diff.getRelevantFileName(), e);
        diffsFailedPaths.add(diff.getRelevantFileName());
      } finally {
        decrementTasks();
      }
    }
  }

  public Future<?> put(Diff diff) {
    if (refactoredPaths.add(diff.getRelevantFileName())) {
      runState.incrementAndGet();
      return workerService.submit(new Task(diff));
    }
    return null;
  }
}
