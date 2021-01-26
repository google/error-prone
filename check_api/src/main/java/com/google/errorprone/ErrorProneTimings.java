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
import com.google.errorprone.matchers.Suppressible;
import com.sun.tools.javac.util.Context;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

  private final Map<String, Stopwatch> timers = new HashMap<>();

  /** Creates a timing span for the given {@link Suppressible}. */
  public AutoCloseable span(Suppressible suppressible) {
    String key = suppressible.canonicalName();
    Stopwatch sw = timers.computeIfAbsent(key, k -> Stopwatch.createUnstarted()).start();
    return () -> sw.stop();
  }

  /** Returns the elapsed durations of each timer. */
  public ImmutableMap<String, Duration> timings() {
    return timers.entrySet().stream()
        .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue().elapsed()));
  }
}
