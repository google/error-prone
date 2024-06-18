/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.hubspot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneTimings;
import com.google.errorprone.matchers.Suppressible;
import com.sun.tools.javac.util.Context;

public class HubSpotMetrics {
  public static synchronized HubSpotMetrics instance(Context context) {
    HubSpotMetrics metrics = context.get(HubSpotMetrics.class);

    if (metrics == null) {
      metrics = create(context);
      context.put(HubSpotMetrics.class, metrics);
    }

    return metrics;
  }

  private static HubSpotMetrics create(Context context) {
    HubSpotMetrics metrics = new HubSpotMetrics(Suppliers.memoize(() -> FileManager.instance(context)));

    HubSpotLifecycleManager.instance(context).addShutdownListener(metrics::write);

    return metrics;
  }

  private final ErrorState errors;
  private final AtomicLongMap<String> timings;
  private final Supplier<FileManager> fileManagerSupplier;


  HubSpotMetrics(Supplier<FileManager> fileManagerSupplier) {
    this.errors = new ErrorState();
    this.timings = AtomicLongMap.create();
    this.fileManagerSupplier = fileManagerSupplier;
  }

  public void recordError(Suppressible s, Map<String, ?> t) {
    errors.exceptions.put(s.canonicalName(), t);
  }

  public void recordMissingCheck(String checkName) {
    errors.missing.add(checkName);
  }

  public void recordTimings(Context context) {
    ErrorProneTimings.instance(context)
        .timings()
        .forEach((k, v) -> timings.put(k, v.toMillis()));
  }

  public void recordListenerDescribeError(DescriptionListener listener, Throwable t) {
    errors.listenerOnDescribeErrors.put(listener.getClass().getCanonicalName(), getErrorDescription(t));
  }

  public void recordUncaughtException(Throwable throwable) {
    errors.unhandledErrors.add(getErrorDescription(throwable));

    fileManagerSupplier.get().getUncaughtExceptionPath().ifPresent(p -> {
      // this should only ever be called once so overwriting is fine
      try {
        Files.write(p, Throwables.getStackTraceAsString(throwable).getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        RuntimeException ex = new RuntimeException("Failed to record uncaught exception", e);
        ex.addSuppressed(throwable);
        throw ex;
      }
    });
  }

  public void recordCheckLoadError(String name, Throwable t) {
    errors.initErrors.put(name, getErrorDescription(t));
  }

  public void recordListenerInitError(String name, Throwable t) {
    errors.listenerInitErrors.put(name, getErrorDescription(t));
  }

  private void write() {
    FileManager fileManager = fileManagerSupplier.get();
    fileManager.getErrorOutputPath().ifPresent(p -> fileManager.write(errors, p));
    fileManager.getTimingsOutputPath().ifPresent(p -> fileManager.write(computeFinalTimings(), p));
  }

  private Map<String, Long> computeFinalTimings() {
    return ImmutableMap.<String, Long>builder()
        .putAll(timings.asMap())
        .put("total", timings.sum())
        .build().entrySet().stream()
        .sorted(Entry.<String, Long>comparingByValue().reversed().thenComparing(Entry.comparingByKey()))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class ErrorState {
    @JsonProperty("errorProneExceptions")
    public final Multimap<String, Map<String, ?>> exceptions = Multimaps.synchronizedMultimap(HashMultimap.create());

    @JsonProperty("errorProneMissingChecks")
    public final Set<String> missing = ConcurrentHashMap.newKeySet();

    @JsonProperty("errorProneInitErrors")
    public final Multimap<String, Map<String, ?>> initErrors = Multimaps.synchronizedMultimap(HashMultimap.create());

    @JsonProperty("errorProneListenerInitErrors")
    public final Multimap<String, Map<String, ?>> listenerInitErrors = Multimaps.synchronizedMultimap(HashMultimap.create());

    @JsonProperty("errorProneListenerDescribeErrors")
    public final Multimap<String, Map<String, ?>> listenerOnDescribeErrors = Multimaps.synchronizedMultimap(HashMultimap.create());

    @JsonProperty("errorProneUnhandledErrors")
    public final Set<Map<String, ?>> unhandledErrors = ConcurrentHashMap.newKeySet();
  }

  public static Map<String, ?> getErrorDescription(Throwable t) {
    return ImmutableMap.<String, String>builder()
        .put("message", t.getMessage())
        .put("class", t.getClass().getCanonicalName())
        .put("stackTrace", Throwables.getStackTraceAsString(t))
        .build();
  }
}
