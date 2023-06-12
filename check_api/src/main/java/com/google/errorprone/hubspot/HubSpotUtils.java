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
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
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
  private static final String ERROR_REPORTING_FLAG = "hubspot:error-reporting";
  private static final String GENERATED_SOURCES_FLAG = "hubspot:generated-sources-pattern";
  private static final Supplier<PathMatcher> GENERATED_PATTERN = VisitorState.memoize(getGeneratedPathsMatcher());

  private static Optional<HubSpotMetrics> METRICS = Optional.empty();

  // TODO: this is hacky
  public static void init(JavacTask task) {
    Context context = ((BasicJavacTask) task).getContext();
    METRICS = Optional.of(HubSpotMetrics.instance(context));
  }

  public static ScannerSupplier createScannerSupplier(Iterable<BugChecker> extraBugCheckers) {
    ImmutableList.Builder<BugCheckerInfo> builder = ImmutableList.builder();
    Iterator<BugChecker> iter = extraBugCheckers.iterator();
    while (iter.hasNext()) {
      try {
        Class<? extends BugChecker> checker = iter.next().getClass();
        builder.add(BugCheckerInfo.create(checker));
      } catch (Throwable e) {
        METRICS.ifPresent(metrics -> metrics.recordCheckLoadError(e));
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
        METRICS.ifPresent(metrics -> metrics.recordListenerInitError(t));
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

  public static void recordUncaughtException(Throwable throwable) {
    METRICS.ifPresent(metrics -> metrics.recordUncaughtException(throwable));
  }

  public static void recordMissingCheck(String checkName) {
    METRICS.ifPresent(metrics -> metrics.recordMissingCheck(checkName));
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

  public static String getPhase(Context context) {
    ErrorProneOptions epOptions = context.get(ErrorProneOptions.class);
    if (epOptions == null) {
      // Happens in some EP tests
      return  "test-compile";
    } else {
      return epOptions.isTestOnlyTarget() ? "test-compile" : "compile";
    }
  }
  private HubSpotUtils() {
    throw new AssertionError();
  }

}
