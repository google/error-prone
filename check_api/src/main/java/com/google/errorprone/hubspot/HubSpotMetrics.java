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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneTimings;
import com.google.errorprone.matchers.Suppressible;
import com.sun.tools.javac.util.Context;

public class HubSpotMetrics {

  enum ErrorType {
    EXCEPTIONS,
    MISSING,
    INIT_ERRORS,
    LISTENER_INIT_ERRORS,
    LISTENER_ON_DESCRIBE_ERROR,
    UNHANDLED_ERRORS
  }

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

  private final ConcurrentHashMap<ErrorType, Set<String>> errors;
  private final AtomicLongMap<String> timings;

  private final Supplier<FileManager> fileManagerSupplier;


  HubSpotMetrics(Supplier<FileManager> fileManagerSupplier) {
    this.errors = new ConcurrentHashMap<>();
    this.timings = AtomicLongMap.create();
    this.fileManagerSupplier = fileManagerSupplier;
  }

  public void recordError(Suppressible s) {
    errors.computeIfAbsent(ErrorType.EXCEPTIONS, ignored -> ConcurrentHashMap.newKeySet())
        .add(s.canonicalName());
  }

  public void recordMissingCheck(String checkName) {
    errors.computeIfAbsent(ErrorType.MISSING, ignored -> ConcurrentHashMap.newKeySet())
        .add(checkName);
  }

  public void recordTimings(Context context) {
    ErrorProneTimings.instance(context)
        .timings()
        .forEach((k, v) -> timings.put(k, v.toMillis()));
  }

  public void recordListenerDescribeError(DescriptionListener listener, Throwable t) {
    errors.computeIfAbsent(ErrorType.LISTENER_ON_DESCRIBE_ERROR, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(t));
  }

  public void recordUncaughtException(Throwable throwable) {
    errors.computeIfAbsent(ErrorType.UNHANDLED_ERRORS, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(throwable));

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

  public void recordCheckLoadError(Throwable t) {
    errors.computeIfAbsent(ErrorType.INIT_ERRORS, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(t));
  }

  public void recordListenerInitError(Throwable t) {
    errors.computeIfAbsent(ErrorType.LISTENER_INIT_ERRORS, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(t));
  }

  private static String toErrorMessage(Throwable e) {
    if (Strings.isNullOrEmpty(e.getMessage())) {
      return "Unknown error";
    } else {
      return e.getMessage();
    }
  }

  private void write() {
    FileManager fileManager = fileManagerSupplier.get();
    fileManager.getErrorOutputPath().ifPresent(p -> fileManager.write(ImmutableMap.of("error-prone-errors-" + fileManager.getPhase(), errors), p));
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

}
