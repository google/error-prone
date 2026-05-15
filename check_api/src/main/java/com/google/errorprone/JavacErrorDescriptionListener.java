/*
 * Copyright 2011 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.fixes.AppliedFix;
import com.google.errorprone.fixes.ErrorProneEndPosTable;
import com.google.errorprone.fixes.ErrorPronePosition;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import javax.tools.JavaFileObject;

/**
 * Making our errors appear to the user and break their build.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class JavacErrorDescriptionListener implements DescriptionListener {
  private final Log log;
  private final JavaFileObject sourceFile;
  private final BiFunction<Description, Fix, AppliedFix> fixToAppliedFix;
  private final Context context;

  // When we're trying to refactor using error prone fixes, any error halts compilation of other
  // files. We set this to true when refactoring so we can log every hit without breaking the
  // compile.
  private final boolean dontUseErrors;

  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  // DiagnosticFlag.API ensures that errors are always reported, bypassing 'shouldReport' logic
  // that filters out duplicate diagnostics at the same position, and ensures that
  // ErrorProneAnalyzer can compare the counts of errors reported by Error Prone with the total
  // number of errors reported.
  private static final ImmutableSet<JCDiagnostic.DiagnosticFlag> DIAGNOSTIC_FLAGS =
      ImmutableSet.of(JCDiagnostic.DiagnosticFlag.API);

  private JavacErrorDescriptionListener(
      Log log,
      ErrorProneEndPosTable endPositions,
      JavaFileObject sourceFile,
      Context context,
      boolean dontUseErrors) {
    this.log = log;
    this.sourceFile = sourceFile;
    this.context = context;
    this.dontUseErrors = dontUseErrors;
    checkNotNull(endPositions);
    // Optimization for checks that emit the same fix multiple times. Consider a check that renames
    // all uses of a symbol, and reports the diagnostic on all occurrences of the symbol. This can
    // be useful in environments where diagnostics are only shown on changed lines, but can lead to
    // quadratic behaviour during fix application if we're not careful.
    Map<Fix, AppliedFix> cache = new HashMap<>();
    try {
      CharSequence sourceFileContent = sourceFile.getCharContent(true);
      fixToAppliedFix =
          (description, fix) ->
              cache.computeIfAbsent(
                  fix,
                  f -> {
                    try {
                      return AppliedFix.apply(sourceFileContent, endPositions, f);
                    } catch (SourcePositionException e) {
                      throw e.toErrorProneError(description.checkName, sourceFile);
                    }
                  });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void onDescribed(Description description) {
    ImmutableList<AppliedFix> appliedFixes =
        description.fixes.stream()
            .filter(f -> !shouldSkipImportTreeFix(description.position, f))
            .map(f -> fixToAppliedFix.apply(description, f))
            .filter(Objects::nonNull)
            .collect(toImmutableList());

    String message = messageForFixes(description, appliedFixes);
    // Swap the log's source and the current file's source; then be sure to swap them back later.
    JavaFileObject originalSource = log.useSource(sourceFile);
    try {
      JCDiagnostic.Factory factory = JCDiagnostic.Factory.instance(context);
      DiagnosticPosition pos =
          new JCDiagnostic.SimpleDiagnosticPosition(description.position.getPreferredPosition());
      JCDiagnostic.DiagnosticType type =
          switch (description.severity()) {
            case ERROR ->
                dontUseErrors
                    ? JCDiagnostic.DiagnosticType.WARNING
                    : JCDiagnostic.DiagnosticType.ERROR;
            case WARNING -> JCDiagnostic.DiagnosticType.WARNING;
            case SUGGESTION -> JCDiagnostic.DiagnosticType.NOTE;
          };
      log.report(
          factory.create(
              type,
              /* lintCategory */ null,
              // Make a defensive copy, as JDK at head mutates its arguments.
              EnumSet.copyOf(DIAGNOSTIC_FLAGS),
              log.currentSource(),
              pos,
              MESSAGE_BUNDLE_KEY,
              message));
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }

  // b/79407644: Because AppliedFix doesn't consider imports, just don't display a
  // suggested fix to an ImportTree when the fix reports imports to remove/add. Imports can still
  // be fixed if they were specified via SuggestedFix.replace, for example.
  private static boolean shouldSkipImportTreeFix(ErrorPronePosition position, Fix f) {
    if (position.getTree() != null && !(position.getTree() instanceof ImportTree)) {
      return false;
    }

    return !f.getImportsToAdd().isEmpty() || !f.getImportsToRemove().isEmpty();
  }

  private static String messageForFixes(Description description, List<AppliedFix> appliedFixes) {
    StringBuilder messageBuilder = new StringBuilder(description.getMessage());
    boolean first = true;
    for (AppliedFix appliedFix : appliedFixes) {
      if (first) {
        messageBuilder.append("\nDid you mean ");
      } else {
        messageBuilder.append(" or ");
      }
      if (appliedFix.isRemoveLine()) {
        messageBuilder.append("to remove this line");
      } else {
        messageBuilder.append("'").append((CharSequence) appliedFix.snippet()).append("'");
      }
      first = false;
    }
    if (!first) { // appended at least one suggested fix to the message
      messageBuilder.append("?");
    }
    return messageBuilder.toString();
  }

  static Factory provider(Context context) {
    return (log, compilation) ->
        new JavacErrorDescriptionListener(
            log,
            ErrorProneEndPosTable.create(compilation),
            compilation.getSourceFile(),
            context,
            false);
  }

  static Factory providerForRefactoring(Context context) {
    return (log, compilation) ->
        new JavacErrorDescriptionListener(
            log,
            ErrorProneEndPosTable.create(compilation),
            compilation.getSourceFile(),
            context,
            true);
  }
}
