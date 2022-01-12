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

package com.google.errorprone.hubspot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.ErrorProneTimings;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.descriptionlistener.CustomDescriptionListenerFactory;
import com.google.errorprone.descriptionlistener.DescriptionListenerResources;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;

public class HubSpotUtils {
  private static final Comparator<Map.Entry<String, Long>> TIMING_COMPARATOR = buildTimingComparator();
  private static final JavaType ERROR_DATA_TYPE = JsonUtils.getTypeFactory()
      .constructMapType(
          HashMap.class,
          JsonUtils.getTypeFactory().constructType(String.class),
          JsonUtils.getTypeFactory().constructCollectionType(Set.class, String.class));
  private static final JavaType TIMINGS_DATA_TYPE = JsonUtils.getTypeFactory()
      .constructMapType(
          HashMap.class,
          String.class,
          Long.class);
  private static final String EXCEPTIONS = "errorProneExceptions";
  private static final String MISSING = "errorProneMissingChecks";
  private static final String INIT_ERROR = "errorProneInitErrors";
  private static final String LISTENER_INIT_ERRORS = "errorProneListenerInitErrors";
  private static final String LISTENER_ON_DESCRIBE_ERROR = "errorProneListenerDescribeErrors";
  private static final String ERROR_REPORTING_FLAG = "hubspot:error-reporting";
  private static final String GENERATED_SOURCES_FLAG = "hubspot:generated-sources-pattern";
  private static final Map<String, Set<String>> DATA = loadExistingData();
  private static final Map<String, Long> PREVIOUS_TIMING_DATA = loadExistingTimings();
  private static final Map<String, Long> TIMING_DATA = new ConcurrentHashMap<>();
  private static final Supplier<PathMatcher> GENERATED_PATTERN = VisitorState.memoize(getGeneratedPathsMatcher());

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
    return isFlagEnabled(ERROR_REPORTING_FLAG, resources.getContext().get(ErrorProneFlags.class));
  }

  public static boolean isErrorHandlingEnabled(ErrorProneOptions options) {
    return isFlagEnabled(ERROR_REPORTING_FLAG, options);
  }

  public static boolean isCanonicalSuppressionEnabled(VisitorState visitorState) {
    return isFlagEnabled("hubspot:canonical-suppressions-only", visitorState.errorProneOptions());
  }

  public static boolean isGeneratedCodeInspectionEnabled(VisitorState visitorState) {
    return isFlagEnabled("hubspot:generated-code-inspection", visitorState.errorProneOptions());
  }

  public static boolean isGenerated(VisitorState state) {
    return GENERATED_PATTERN
        .get(state)
        .matches(Paths.get(ASTHelpers.getFileName(state.getPath().getCompilationUnit())));
  }

  public static void recordError(Suppressible s) {
    DATA.computeIfAbsent(EXCEPTIONS, ignored -> ConcurrentHashMap.newKeySet())
        .add(s.canonicalName());
  }

  public static void recordMissingCheck(String checkName) {
    DATA.computeIfAbsent(MISSING, ignored -> ConcurrentHashMap.newKeySet())
        .add(checkName);
  }

  public static void recordTimings(Context context) {
    ErrorProneTimings.instance(context)
        .timings()
        .forEach((k, v) -> TIMING_DATA.put(k, v.toMillis()));
  }

  public static void recordListenerDescribeError(DescriptionListener listener, Throwable t) {
    DATA.computeIfAbsent(LISTENER_ON_DESCRIBE_ERROR, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(t));
  }

  public static void init(JavacTask task) {
    Context context = ((BasicJavacTask) task).getContext();
    HubSpotLifecycleManager.instance(context).addShutdownListener(() -> {
      FileManager.getErrorOutputPath().ifPresent(p -> FileManager.write(DATA, p));
      FileManager.getTimingsOutputPath().ifPresent(p -> FileManager.write(computeFinalTimings(), p));
    });
  }

  public static void recordUncaughtException(Throwable throwable) {
    FileManager.getUncaughtExceptionPath().ifPresent(p -> {
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

  private static Supplier<PathMatcher> getGeneratedPathsMatcher() {
    return visitorState -> Optional.ofNullable(visitorState.errorProneOptions().getFlags())
        .flatMap(f -> f.get(GENERATED_SOURCES_FLAG))
        .map(s -> FileSystems.getDefault().getPathMatcher(s))
        .orElseThrow(() -> new IllegalStateException("Must specify flag " + GENERATED_SOURCES_FLAG));
  }

  private static boolean isFlagEnabled(String flag, ErrorProneOptions errorProneOptions) {
    return isFlagEnabled(flag, errorProneOptions.getFlags());
  }

  private static boolean isFlagEnabled(String flag, ErrorProneFlags flags) {
    if (flags == null) {
      return false;
    }

    return flags
        .getBoolean(flag)
        .orElse(false);
  }

  private static void recordCheckLoadError(Throwable t) {
    DATA.computeIfAbsent(INIT_ERROR, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(t));
  }

  private static void recordListenerInitError(Throwable t) {
    DATA.computeIfAbsent(LISTENER_INIT_ERRORS, ignored -> ConcurrentHashMap.newKeySet())
        .add(toErrorMessage(t));
  }

  private static String toErrorMessage(Throwable e) {
    if (Strings.isNullOrEmpty(e.getMessage())) {
      return "Unknown error";
    } else {
      return e.getMessage();
    }
  }

  private static Map<String, Long> computeFinalTimings() {
    Map<String, Long> res = new HashMap<>(PREVIOUS_TIMING_DATA);
    TIMING_DATA.forEach(
        (k, newValue) -> res.compute(
            k,
            (key, oldValue) -> oldValue == null ? newValue : oldValue + newValue));

    res.put(
        "total",
        res.entrySet()
            .stream()
            .filter(e -> !e.getKey().equals("total"))
            .mapToLong(Map.Entry::getValue)
            .sum());

    return res.entrySet()
        .stream()
        .sorted(TIMING_COMPARATOR)
        .collect(ImmutableMap.toImmutableMap(
            Map.Entry::getKey,
            Map.Entry::getValue));
  }

  private static Map<String, Set<String>> loadExistingData() {
    return FileManager.getErrorOutputPath()
        .map(HubSpotUtils::loadData)
        .map(HubSpotUtils::toDataSet)
        .orElseGet(ConcurrentHashMap::new);
  }

  private static Map<String, Long> loadExistingTimings() {
    return FileManager.getTimingsOutputPath()
        .map(HubSpotUtils::loadTimingData)
        .orElseGet(ImmutableMap::of);
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
    return loadData(path, ERROR_DATA_TYPE);
  }

  private static Map<String, Long> loadTimingData(Path path) {
    return loadData(path, TIMINGS_DATA_TYPE);
  }

  private static <T, U> Map<T, U> loadData(Path path, JavaType type) {
    if (!Files.exists(path)) {
      return ImmutableMap.of();
    }

    try {
      return JsonUtils.getMapper().readValue(path.toFile(), type);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read existing file to load data", e);
    }
  }

  private static Comparator<Map.Entry<String, Long>> buildTimingComparator() {
    Comparator<Map.Entry<String, Long>> comp = Comparator.comparing(Map.Entry::getValue);
    return comp.reversed().thenComparing(Map.Entry::getKey);
  }

  private HubSpotUtils() {
    throw new AssertionError();
  }
}
