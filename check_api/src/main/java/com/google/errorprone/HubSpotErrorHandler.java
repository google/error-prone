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

public class HubSpotErrorHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String EXCEPTIONS = "errorProneExceptions";
  private static final String MISSING = "errorProneMissingChecks";
  private static final String INIT_ERROR = "errorProneInitErrors";
  private static final String LISTENER_INIT_ERRORS = "errorProneListenerInitErrors";
  private static final Map<String, Set<String>> DATA = loadExistingData();

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

  public static boolean isEnabled(DescriptionListenerResources resources) {
    return isEnabled(resources.getContext().get(ErrorProneFlags.class));
  }

  public static boolean isEnabled(ErrorProneOptions options) {
    return isEnabled(options.getFlags());
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

  private static boolean isEnabled(ErrorProneFlags flags) {
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
    Optional<Path> output = getOutput();
    if (!output.isPresent()) {
      return;
    }

    try (OutputStream stream = Files.newOutputStream(output.get())) {
      MAPPER.writeValue(stream, DATA);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to write errorprone metadata to %s", output)
      );
    }
  }

  private static Map<String, Set<String>> loadExistingData() {
    return getOutput()
        .map(HubSpotErrorHandler::loadData)
        .map(HubSpotErrorHandler::toDataSet)
        .orElseGet(ConcurrentHashMap::new);
  }

  private static Map<String, Set<String>> toDataSet(Map<String, Set<String>> data) {
    ConcurrentHashMap<String, Set<String>> map = new ConcurrentHashMap<>();
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

  private static Optional<Path> getOutput() {
    return getOutputDir().map(o -> o.resolve("error-prone-exceptions.json"));
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

  private HubSpotErrorHandler() {
    throw new AssertionError();
  }
}
