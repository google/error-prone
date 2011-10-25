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

import com.google.errorprone.matchers.ErrorChecker.AstError;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * Report errors via the javac annotation processor.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class JSR269ErrorReporter implements ErrorReporter {
  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  private final Log log;
  private final Messager messager;
  private final JavaFileObject sourceFile;

  public JSR269ErrorReporter(Log log, Messager messager, JavaFileObject sourceFile) {
    this.log = log;
    this.messager = messager;
    this.sourceFile = sourceFile;
  }

  public void emitError(AstError error) {
    JavaFileObject originalSource;
    originalSource = log.useSource(sourceFile);
    try {
      // Workaround. The first API increments the error count and causes the build to fail.
      // The second API gets the correct line and column number.
      // TODO: figure out how to get both features with one error
      messager.printMessage(Kind.ERROR, "");
      log.error((DiagnosticPosition) error.match, MESSAGE_BUNDLE_KEY, error.message);
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }
}
