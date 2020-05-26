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

package com.google.errorprone.scanner;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Suppressible;
import com.sun.tools.javac.util.Context;

public class HubSpotErrorHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String EXCEPTIONS = "errorProneExceptions";
  private static final String MISSING = "errorProneMissingChecks";
  private static final String INIT_ERROR = "errorProneInitErrors";
  private static final Map<String, Set<String>> DATA = loadExistingData();

  public static ScannerSupplier createScannerSupplier(Iterable<BugChecker> extraBugCheckers) {
    ImmutableList.Builder<BugCheckerInfo> builder = ImmutableList.builder();
    Iterator<BugChecker> iter = extraBugCheckers.iterator();
    while (iter.hasNext()) {
      try {
        Class<? extends BugChecker> checker = iter.next().getClass();
        builder.add(BugCheckerInfo.create(checker));
      } catch (Exception e) {
        recordCheckLoadError(e);
      }
    }

    return ScannerSupplier.fromBugCheckerInfos(builder.build());
  }

  public static boolean isEnabled(ErrorProneOptions options) {
    return options.getFlags()
        .getBoolean("hubspot:error-reporting")
        .orElse(false);
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

  private static void recordCheckLoadError(Exception e) {
    DATA.computeIfAbsent(INIT_ERROR, ignored -> ConcurrentHashMap.newKeySet())
        .add(toInitErrorMessage(e));

    flushErrors();
  }

  private static String toInitErrorMessage(Exception e) {
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
