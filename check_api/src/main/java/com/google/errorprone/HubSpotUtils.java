/*
 * Copyright 2020 The Error Prone Authors.
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.descriptionlistener.CustomDescriptionListenerFactory;
import com.google.errorprone.descriptionlistener.DescriptionListenerResources;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.util.Context;

public class HubSpotUtils {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String EXCEPTIONS = "errorProneExceptions";
  private static final String MISSING = "errorProneMissingChecks";
  private static final String INIT_ERROR = "errorProneInitErrors";
  private static final String LISTENER_INIT_ERRORS = "errorProneListenerInitErrors";
  private static final Map<String, Set<String>> DATA = loadExistingData();
  private static final Map<String, Long> PREVIOUS_TIMINGS = loadExistingTimings();
  private static final Map<String, Long> TIMINGS = new ConcurrentHashMap<>();

  public static ScannerSupplier createScannerSupplier(Iterable<BugChecker> extraBugCheckers) {
    ImmutableList.Builder<BugCheckerInfo> builder = ImmutableList.builder();
    Iterator<BugChecker> iter = extraBugCheckers.iterator();
    while (iter.hasNext()) {
      try {
        Class<? extends BugChecker> checker = iter.next().getClass();
        builder.add(BugCheckerInfo.create(checker));
      } catch (Throwable e) {
        recordCheckLoadError(e);
      }
    }

    return ScannerSupplier.fromBugCheckerInfos(builder.build());
  }

  public static List<DescriptionListener> loadDescriptionListeners(Iterable<CustomDescriptionListenerFactory> factories, DescriptionListenerResources resources) {
    Iterator<CustomDescriptionListenerFactory> iter = factories.iterator();
    ImmutableList.Builder<DescriptionListener> listeners = ImmutableList.builder();
    while (iter.hasNext()) {
      try {
        listeners.add(iter.next().createFactory(resources));
      } catch (Throwable t) {
        recordListenerInitError(t);
      }
    }

    return listeners.build();
  }

  public static boolean isErrorHandlingEnabled(DescriptionListenerResources resources) {
    return isErrorHandlingEnabled(resources.getContext().get(ErrorProneFlags.class));
  }

  public static boolean isErrorHandlingEnabled(ErrorProneOptions options) {
    return isErrorHandlingEnabled(options.getFlags());
  }

  public static void recordError(Suppressible s) {
    DATA.computeIfAbsent(EXCEPTIONS, ignored -> ConcurrentHashMap.newKeySet())
        .add(s.canonicalName());

    flushErrors();
  }

  public static void recordMissingCheck(String checkName) {
    DATA.computeIfAbsent(MISSING, ignored -> ConcurrentHashMap.newKeySet())
        .add(checkName);

    flushErrors();
  }

  public static void recordTimings(Context context) {
    ErrorProneTimings.instance(context)
        .timings()
        .forEach((k, v) -> TIMINGS.put(k, v.toMillis()));

    flushTimings();
  }

  private static boolean isErrorHandlingEnabled(ErrorProneFlags flags) {
    if (flags == null) {
      return false;
    }

    return flags
        .getBoolean("hubspot:error-reporting")
        .orElse(false);
  }

  private static void recordCheckLoadError(Throwable t) {
    DATA.computeIfAbsent(INIT_ERROR, ignored -> ConcurrentHashMap.newKeySet())
        .add(toInitErrorMessage(t));

    flushErrors();
  }

  private static void recordListenerInitError(Throwable t) {
    DATA.computeIfAbsent(LISTENER_INIT_ERRORS, ignored -> ConcurrentHashMap.newKeySet())
        .add(toInitErrorMessage(t));

    flushErrors();
  }

  private static String toInitErrorMessage(Throwable e) {
    if (Strings.isNullOrEmpty(e.getMessage())) {
      return "Unknown error";
    } else {
      return e.getMessage();
    }
  }

  private static void flushErrors() {
    getErrorOutput().ifPresent(p -> flush(DATA, p));
  }

  private static void flushTimings() {
    getTimingsOutput().ifPresent(p -> flush(computeFinalTimings(), p));
  }

  private static Map<String, Long> computeFinalTimings() {
    TreeMap<String, Long> res = new TreeMap<>(PREVIOUS_TIMINGS);
    TIMINGS.forEach((k, v) -> {
      if (res.containsKey(k)) {
        res.put(k, res.get(k) + v) ;
      } else {
        res.put(k, v);
      }
    });

    return res;
  }

  private static void flush(Object data, Path path) {
    try (OutputStream stream = Files.newOutputStream(path)) {
      MAPPER.writeValue(stream, data);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to write errorprone metadata to %s", path)
      );
    }
  }

  private static Map<String, Set<String>> loadExistingData() {
    return getErrorOutput()
        .map(HubSpotUtils::loadData)
        .map(HubSpotUtils::toDataSet)
        .orElseGet(ConcurrentHashMap::new);
  }

  private static Map<String, Long> loadExistingTimings() {
    return getTimingsOutput()
        .map(HubSpotUtils::loadTimingData)
        .orElseGet(ConcurrentHashMap::new);
  }

  private static Map<String, Set<String>> toDataSet(Map<String, Set<String>> data) {
    ConcurrentHashMap<String, Set<String>> map = new ConcurrentHashMap<>(data.size());
    data.forEach((k, v) -> {
      Set<String> set = ConcurrentHashMap.newKeySet(v.size());
      set.addAll(v);
      map.put(k, set);
    });

    return map;
  }

  private static Map<String, Set<String>> loadData(Path path) {
    if (!Files.exists(path)) {
      return ImmutableMap.of();
    }

    JavaType type = MAPPER
        .getTypeFactory()
        .constructMapType(
            HashMap.class,
            MAPPER.getTypeFactory().constructType(String.class),
            MAPPER
                .getTypeFactory()
                .constructCollectionType(Set.class, String.class)
        );

    try {
      return MAPPER.readValue(path.toFile(), type);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read existing file to load data", e);
    }
  }

  private static Map<String, Long> loadTimingData(Path path) {
    if (!Files.exists(path)) {
      return ImmutableMap.of();
    }

    JavaType type = MAPPER
        .getTypeFactory()
        .constructMapType(
            HashMap.class,
            String.class,
            Long.class
        );

    try {
      return MAPPER.readValue(path.toFile(), type);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read existing file to load timing data", e);
    }
  }

  private static Optional<Path> getErrorOutput() {
    return getOutputDir().map(o -> o.resolve("error-prone-exceptions.json"));
  }

  private static Optional<Path> getTimingsOutput() {
    return isTimingEnabled() ?
      getOutputDir().map(o -> o.resolve("error-prone-timings.json")) : Optional.empty();
  }

  private static boolean isTimingEnabled() {
    return "true".equalsIgnoreCase(System.getenv("ERROR_PRONE_TIMINGS_ENABLED"));
  }

  private static Optional<Path> getOutputDir() {
    String dir = System.getenv("MAVEN_PROJECTBASEDIR");
    if (Strings.isNullOrEmpty(dir)) {
      return Optional.empty();
    }

    Path res = Paths.get(dir).resolve("target/overwatch-metadata");
    if (!Files.exists(res)) {
      try {
        Files.createDirectories(res);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to create directory: %s", res),
            e
        );
      }
    }

    return Optional.of(res);
  }

  private HubSpotUtils() {
    throw new AssertionError();
  }
}
