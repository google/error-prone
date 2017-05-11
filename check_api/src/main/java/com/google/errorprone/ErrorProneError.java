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

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import javax.tools.JavaFileObject;

/**
 * Wraps an unrecoverable error that occurs during analysis with the source position that triggered
 * the crash.
 */
public class ErrorProneError extends Error {

  private final String checkName;
  private final Throwable cause;
  private final DiagnosticPosition pos;
  private final JavaFileObject source;

  public ErrorProneError(
      String checkName, Throwable cause, DiagnosticPosition pos, JavaFileObject source) {
    super(
        formatMessage(checkName, source, pos, cause),
        cause,
        /*enableSuppression=*/ true,
        /*writableStackTrace=*/ false);
    this.checkName = checkName;
    this.cause = cause;
    this.pos = pos;
    this.source = source;
  }

  public void logFatalError(Log log) {
    String version = ErrorProneVersion.loadVersionFromPom().or("unknown version");
    JavaFileObject prev = log.currentSourceFile();
    try {
      log.useSource(source);
      log.error(pos, "error.prone.crash", Throwables.getStackTraceAsString(cause), version);
    } finally {
      log.useSource(prev);
    }
  }

  private static String formatMessage(
      String checkName, JavaFileObject file, DiagnosticPosition pos, Throwable cause) {
    DiagnosticSource source = new DiagnosticSource(file, /*log=*/ null);
    int column = source.getColumnNumber(pos.getStartPosition(), /*expandTabs=*/ true);
    int line = source.getLineNumber(pos.getStartPosition());
    String snippet = source.getLine(pos.getStartPosition());
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "\n%s:%d: %s: An exception was thrown by Error Prone: %s\n",
            source.getFile().getName(), line, checkName, cause.getMessage()));
    sb.append(snippet).append('\n');
    if (column > 0) {
      sb.append(Strings.repeat(" ", column - 1));
    }
    sb.append("^\n");
    return sb.toString();
  }

  public String checkName() {
    return checkName;
  }
}
