/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Suppressible;
import com.sun.tools.javac.util.Context;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongBinaryOperator;
import java.util.function.ToLongFunction;

/** A collection of timing data for the runtime of individual checks. */
public final class ErrorProneTimings {

  private static final Context.Key<ErrorProneTimings> timingsKey = new Context.Key<>();

  public static ErrorProneTimings instance(Context context) {
    ErrorProneTimings instance = context.get(timingsKey);
    if (instance == null) {
      instance = new ErrorProneTimings(context);
    }
    return instance;
  }

  private ErrorProneTimings(Context context) {
    context.put(timingsKey, this);
  }

  /**
   * The checks seen so far, in the order they first ran.
   *
   * <p>A canonical name maps to every {@link CheckTiming} reported under it, so two checks sharing
   * one name are reported as their sum rather than as whichever ran last.
   */
  private final Map<String, List<CheckTiming>> timings = new LinkedHashMap<>();

  /**
   * Timing state for a {@link Suppressible} that is not a {@link BugChecker} and owns no field.
   *
   * <p>Keyed by canonical name, so the map holds one entry per name a compilation reports and needs
   * no equality contract from {@link Suppressible}, which declares none.
   */
  private final Map<String, CheckTiming> unownedTimings = new LinkedHashMap<>();

  private final Stopwatch initializationTime = Stopwatch.createUnstarted();

  /**
   * Starts timing one invocation of the given {@link Suppressible}, and returns the state to close
   * when the invocation finishes.
   *
   * <p>The returned value is state this collection keeps rather than a fresh object, so the caller
   * must close it before another invocation reporting the same canonical name begins, and must not
   * retain it.
   */
  public AutoCloseable span(Suppressible suppressible) {
    CheckTiming timing =
        suppressible instanceof BugChecker bugChecker
            ? bugChecker.checkTiming()
            : unownedTimings.computeIfAbsent(
                suppressible.canonicalName(), unused -> new CheckTiming());
    if (timing.claim(this)) {
      timings.computeIfAbsent(suppressible.canonicalName(), unused -> new ArrayList<>()).add(timing);
    }
    timing.begin();
    return timing;
  }

  /** Creates a timing span for initialization. */
  public AutoCloseable initializationTimeSpan() {
    initializationTime.start();
    return () -> initializationTime.stop();
  }

  /** Returns how long each check ran, estimated as {@link CheckTiming#elapsed} describes. */
  public ImmutableMap<String, Duration> timings() {
    return timings.entrySet().stream()
        .collect(
            toImmutableMap(
                e -> e.getKey(),
                e ->
                    e.getValue().stream()
                        .map(CheckTiming::elapsed)
                        .reduce(Duration.ZERO, Duration::plus)));
  }

  /** Returns the longest timed invocation of each check, in nanoseconds. */
  public ImmutableMap<String, Long> maxNanos() {
    return fold(CheckTiming::maxNanos, Math::max);
  }

  /** Returns how many times each check ran. */
  public ImmutableMap<String, Long> counts() {
    return fold(CheckTiming::count, Long::sum);
  }

  private ImmutableMap<String, Long> fold(
      ToLongFunction<CheckTiming> value, LongBinaryOperator combine) {
    return timings.entrySet().stream()
        .collect(
            toImmutableMap(
                e -> e.getKey(),
                e -> {
                  long result = 0;
                  for (CheckTiming timing : e.getValue()) {
                    result = combine.applyAsLong(result, value.applyAsLong(timing));
                  }
                  return result;
                }));
  }

  /** Returns the elapsed initialization time. */
  public Duration initializationTime() {
    return initializationTime.elapsed();
  }
}
