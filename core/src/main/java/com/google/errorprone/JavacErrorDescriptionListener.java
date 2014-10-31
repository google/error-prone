/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.errorprone.fixes.AppliedFix;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import java.io.IOError;
import java.io.IOException;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * Making our errors appear to the user and break their build.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class JavacErrorDescriptionListener implements DescriptionListener {
  private final Log log;
  private final EndPosTable endPositions;
  private final JavaFileObject sourceFile;
  private final CharSequence sourceFileContent;

  private final Function<Fix, AppliedFix> fixToAppliedFix = new Function<Fix, AppliedFix>() {
    @Override
    public AppliedFix apply(Fix fix) {
      return AppliedFix.fromSource(sourceFileContent, endPositions).apply(fix);
    }
  };

  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  public JavacErrorDescriptionListener(
      Log log,
      EndPosTable endPositions,
      JavaFileObject sourceFile) {
    this.log = log;
    this.endPositions = checkNotNull(endPositions);
    this.sourceFile = sourceFile;
    try {
      this.sourceFileContent = sourceFile.getCharContent(true);
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  @Override
  public void onDescribed(Description description) {
    // Swap the log's source and the current file's source; then be sure to swap them back later.
    JavaFileObject originalSource = log.useSource(sourceFile);

    List<AppliedFix> appliedFixes = Lists.transform(description.fixes, fixToAppliedFix);
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
        messageBuilder.append("'" + appliedFix.getNewCodeSnippet() + "'");
      }
      first = false;
    }
    if (!first) {     // appended at least one suggested fix to the message
      messageBuilder.append("?");
    }
    final String message = messageBuilder.toString();

    switch (description.severity) {
      case ERROR:
        log.error((DiagnosticPosition) description.node, MESSAGE_BUNDLE_KEY, message);
        break;
      case WARNING:
        log.warning((DiagnosticPosition) description.node, MESSAGE_BUNDLE_KEY, message);
        break;
      default:
        break;
    }

    if (originalSource != null) {
      log.useSource(originalSource);
    }
  }
}
