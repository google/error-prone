/*
 * Copyright 2018 The Error Prone Authors.
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

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Generates a parseable report file.
 */
public class FileReporter implements TaskListener {

  PrintWriter pw;

  public FileReporter(ErrorProneOptions options) {
    try {
      final String reportFile = options.getReportFile();
      if (reportFile != null && !reportFile.isEmpty()) {
        pw = new PrintWriter(new FileWriter(options.getReportFile()));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void started(final TaskEvent e) {
    if (e.getKind() == TaskEvent.Kind.COMPILATION) {
      if (pw != null) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.println("<errorprone timestamp=\"" + ZonedDateTime.now() + "\">");
        pw.println("  <descriptions>");
      }
    }
  }

  @Override
  public void finished(final TaskEvent e) {
    if (e.getKind() == TaskEvent.Kind.COMPILATION) {
      if (pw != null) {
        pw.println("  </descriptions>");
        pw.println("</errorprone>");
        pw.close();
      }
    }
  }

  public void report(final JCDiagnostic.DiagnosticType type, final DiagnosticSource source,
                     final JCDiagnostic.DiagnosticPosition pos, final String message) {
    if (pw != null) {
      pw.println("    <description");
      pw.println("      type=\"" + type + "\"");
      pw.println("      source=\"" + source.getFile().getName() + "\"");
      pw.println("      line=\"" + source.getLineNumber(pos.getPreferredPosition()) + "\"");
      pw.println("      column=\"" + source.getColumnNumber(pos.getPreferredPosition(),
          false) + "\">");
      pw.println("      <message>" + StringEscapeUtils.escapeXml11(message) + "</message>");
      pw.println("    </description>");
    }
  }
}
