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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URISyntaxException;

import static java.util.Locale.ENGLISH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorFindingCompilerIntegrationTest {

  private DiagnosticCollector<JavaFileObject> diagnostics;

  @Before
  public void setUp() {
    diagnostics = new DiagnosticCollector<JavaFileObject>();
  }

  @Test
  public void testShouldFailToCompileSourceFileWithError() throws Exception {
    assertFalse(new ErrorFindingCompiler(
        sources("PositiveCase1.java"),
        diagnostics,
        ToolProvider.getSystemJavaCompiler())
        .run());
    boolean found = false;
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      String message = diagnostic.getMessage(ENGLISH);
      if (diagnostic.getKind() == Kind.ERROR && message.contains("Preconditions#checkNotNull")) {
        assertThat(message, containsString("\"string literal\""));
        assertThat(diagnostic.getLineNumber(), is(23L));
        assertThat(diagnostic.getColumnNumber(), is(32L));
        found = true;
        return;

      }
    }
    assertTrue("Warning should be found. Diagnostics: " + diagnostics.getDiagnostics(), found);
  }

  private String[] sources(String... files) throws URISyntaxException {
    String[] result = new String[files.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = new File(getClass().getResource("/" + files[i]).toURI()).getAbsolutePath();
    }
    return result;
  }
}
