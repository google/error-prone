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

import com.google.errorprone.matchers.ErrorProducingMatcher.AstError;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import javax.tools.JavaFileObject;
import java.io.IOException;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class LogReporter implements ErrorReporter {
  private final Log log;
  private final JavaFileObject sourceFile;

  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  public LogReporter(Log log, JavaFileObject sourceFile) {
    this.log = log;
    this.sourceFile = sourceFile;
  }

  @Override
  public void emitError(AstError error) {
    JavaFileObject originalSource;
    // Swap the log's source and the current file's source; then be sure to swap them back later.
    originalSource = log.useSource(sourceFile);
    try {
      CharSequence content = sourceFile.getCharContent(true);
      log.error((DiagnosticPosition) error.match, MESSAGE_BUNDLE_KEY, error.message
          + "\nDid you mean to replace \""
          + content.subSequence(error.suggestedFix.startPosition, error.suggestedFix.endPosition)
          + "\" with \"" + error.suggestedFix.replaceWith + "\"");
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }
}
