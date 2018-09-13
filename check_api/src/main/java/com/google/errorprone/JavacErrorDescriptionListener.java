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

import com.google.errorprone.fixes.AppliedFix;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;

/**
 * Making our errors appear to the user and break their build.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class JavacErrorDescriptionListener implements DescriptionListener {
  private final Log log;
  private final JavaFileObject sourceFile;
  private final Function<Fix, AppliedFix> fixToAppliedFix;
  private final Context context;

  // When we're trying to refactor using error prone fixes, any error halts compilation of other
  // files. We set this to true when refactoring so we can log every hit without breaking the
  // compile.
  private final boolean dontUseErrors;

  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  private JavacErrorDescriptionListener(
      Log log,
      EndPosTable endPositions,
      JavaFileObject sourceFile,
      Context context,
      boolean dontUseErrors) {
    this.log = log;
    this.sourceFile = sourceFile;
    this.context = context;
    this.dontUseErrors = dontUseErrors;
    checkNotNull(endPositions);
    try {
      CharSequence sourceFileContent = sourceFile.getCharContent(true);
      fixToAppliedFix = fix -> AppliedFix.fromSource(sourceFileContent, endPositions).apply(fix);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void onDescribed(Description description) {
    List<AppliedFix> appliedFixes =
        description.fixes.stream()
            .filter(f -> !shouldSkipImportTreeFix(description.position, f))
            .map(fixToAppliedFix)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));

    String message = messageForFixes(description, appliedFixes);
    // Swap the log's source and the current file's source; then be sure to swap them back later.
    JavaFileObject originalSource = log.useSource(sourceFile);
    try {
      JCDiagnostic.Factory factory = JCDiagnostic.Factory.instance(context);
      JCDiagnostic.DiagnosticType type = JCDiagnostic.DiagnosticType.ERROR;
      DiagnosticPosition pos = description.position;
      switch (description.severity) {
        case ERROR:
          if (dontUseErrors) {
            type = JCDiagnostic.DiagnosticType.WARNING;
          } else {
            type = JCDiagnostic.DiagnosticType.ERROR;
          }
          break;
        case WARNING:
          type = JCDiagnostic.DiagnosticType.WARNING;
          break;
        case SUGGESTION:
          type = JCDiagnostic.DiagnosticType.NOTE;
          break;
      }
      log.report(factory.create(type, log.currentSource(), pos, MESSAGE_BUNDLE_KEY, message));
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }

  // b/79407644: Because AppliedFix doesn't consider imports, just don't display a
  // suggested fix to an ImportTree when the fix reports imports to remove/add. Imports can still
  // be fixed if they were specified via SuggestedFix.replace, for example.
  private static boolean shouldSkipImportTreeFix(DiagnosticPosition position, Fix f) {
    if (position.getTree() != null && position.getTree().getKind() != Kind.IMPORT) {
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
        messageBuilder.append("'").append(appliedFix.getNewCodeSnippet()).append("'");
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
            log, compilation.endPositions, compilation.getSourceFile(), context, false);
  }

  static Factory providerForRefactoring(Context context) {
    return (log, compilation) ->
        new JavacErrorDescriptionListener(
            log, compilation.endPositions, compilation.getSourceFile(), context, true);
  }
}
