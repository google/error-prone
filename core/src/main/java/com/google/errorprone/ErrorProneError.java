/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.base.Throwables;

import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import javax.tools.JavaFileObject;

/**
 * Wraps an unrecoverable error that occurrs during analysis with the source
 * position that triggered the crash.
 */
public class ErrorProneError extends Error {

  private final Throwable cause;
  private final DiagnosticPosition pos;
  private final JavaFileObject source;

  public ErrorProneError(Throwable cause, DiagnosticPosition pos, JavaFileObject source) {
    this.cause = cause;
    this.pos = pos;
    this.source = source;
  }

  public void logFatalError(Log log) {
    String version = ErrorProneCompiler.loadVersionFromPom().or("unknown version");
    JavaFileObject prev = log.currentSourceFile();
    try {
      log.useSource(source);
      log.error(
          pos, "error.prone.crash", Throwables.getStackTraceAsString(cause), version);
    } finally {
      log.useSource(prev);
    }
  }
}
