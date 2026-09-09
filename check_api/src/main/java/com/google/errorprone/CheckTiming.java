/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone;

import java.io.Serializable;
import java.time.Duration;

/**
 * How long one check ran during one compilation, and how many times.
 *
 * <p>A {@link com.google.errorprone.bugpatterns.BugChecker} owns its instance; every other {@link
 * com.google.errorprone.matchers.Suppressible} shares one per canonical name with {@link
 * ErrorProneTimings}. Either way {@link ErrorProneTimings} claims the instance for the compilation
 * that is running.
 *
 * <p>The count is exact. The elapsed time is an estimate: a check whose invocations are shorter than
 * a clock read is timed on a sample of them, and each sample counts for the invocations it stands in
 * for.
 *
 * <p>An instance is confined to one thread, and {@link #begin} rejects a span opened inside another
 * span on the same instance. javac is single-threaded and a check never runs inside itself, so
 * nothing here is synchronized.
 */
public final class CheckTiming implements AutoCloseable, Serializable {

  /**
   * Invocation count below which every invocation is timed.
   *
   * <p>Above it, one invocation in {@code count / SAMPLES_PER_STRIDE} is timed, so a check that runs
   * a million times contributes a few thousand samples rather than a million clock reads.
   */
  private static final long SAMPLES_PER_STRIDE = 256;

  /**
   * Mean cost, in nanoseconds, above which a check is timed on every invocation.
   *
   * <p>A pair of {@link System#nanoTime} calls costs a few tens of nanoseconds, so timing an
   * invocation that lasts a microsecond perturbs it by a few percent. Raising this bound would sample
   * the checks a report is read for: a check that visits a hundred thousand nodes and takes a second
   * averages about ten microseconds per invocation.
   */
  private static final long TIME_EVERY_INVOCATION_ABOVE_NANOS = 1_000;

  private static final long serialVersionUID = 1L;

  /**
   * The {@link ErrorProneTimings} this state belongs to.
   *
   * <p>A scanner can outlive the compilation that built it, so a check whose instance is reused
   * starts from zero rather than accumulating across compilations. Two compilations that share one
   * scanner and run at the same time would race here; nothing in Error Prone shares a scanner that
   * way, and {@link com.google.errorprone.scanner.ScannerSupplier#fromScanner} is the one entry
   * point that lets an embedder do it.
   */
  private transient Object owner;

  private transient long count;
  private transient long stride;
  private transient long nextSample;
  private transient long weightedNanos;
  private transient long sampledNanos;
  private transient long sampledCount;
  private transient long maxNanos;
  private transient long startNanos;
  private transient boolean sampling;
  private transient boolean open;

  boolean claim(Object newOwner) {
    if (owner == newOwner) {
      return false;
    }
    owner = newOwner;
    count = 0;
    stride = 1;
    nextSample = 1;
    weightedNanos = 0;
    sampledNanos = 0;
    sampledCount = 0;
    maxNanos = 0;
    sampling = false;
    open = false;
    return true;
  }

  /**
   * Records that an invocation has started, and reads the clock if this one is being timed.
   *
   * @throws IllegalStateException if a span on this check is already open
   */
  void begin() {
    if (open) {
      throw new IllegalStateException("a timing span for this check is already open; close it before opening another");
    }
    open = true;
    long n = ++count;
    if (n < nextSample) {
      sampling = false;
      return;
    }
    sampling = true;
    startNanos = System.nanoTime();
  }

  /** Returns whether {@link #begin} chose to time the invocation that is open. */
  boolean sampled() {
    return sampling;
  }

  /** Records the end of the invocation {@link #begin} started, timing it if it was sampled. */
  @Override
  public void close() {
    // The clock is read only for an invocation begin() chose to time.
    closeWith(sampling ? System.nanoTime() - startNanos : 0);
  }

  /**
   * Records the end of the open invocation, as {@link #close} does, with the elapsed time supplied
   * rather than measured.
   */
  void closeWith(long elapsedNanos) {
    open = false;
    if (!sampling) {
      return;
    }
    // A span records once, so a second close takes the return above.
    sampling = false;
    weightedNanos += elapsedNanos * stride;
    sampledNanos += elapsedNanos;
    sampledCount++;
    maxNanos = Math.max(maxNanos, elapsedNanos);
    stride =
        sampledNanos / sampledCount >= TIME_EVERY_INVOCATION_ABOVE_NANOS
            ? 1
            : Math.max(1, count / SAMPLES_PER_STRIDE);
    nextSample = count + stride;
  }

  /** Returns how long this check ran, estimated from the invocations that were timed. */
  public Duration elapsed() {
    return Duration.ofNanos(weightedNanos);
  }

  /** Returns exactly how many times this check ran. */
  public long count() {
    return count;
  }

  /**
   * Returns the longest timed invocation, in nanoseconds.
   *
   * <p>A check whose total is large while this value is small is expensive on every invocation. One
   * whose total is close to this value paid a one-off cost, such as the first lookup of a type that
   * the compilation classpath does not have, and is cheap the rest of the time.
   */
  public long maxNanos() {
    return maxNanos;
  }
}
