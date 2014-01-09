/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.suppress;

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.DiagnosticTestHelper;
import com.google.errorprone.ErrorProneCompiler;
import com.google.errorprone.ErrorProneScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ReturnTree;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Test for unsuppressible checks.
 *
 * @author eaftan@google.com (Eddie Aftandilan)
 */
public class UnsuppressibleTest {

  @BugPattern(name = "MyChecker",
      summary = "Test checker that is unsuppressible",
      explanation = "Test checker that that is unsuppressible",
      suppressibility = Suppressibility.UNSUPPRESSIBLE,
      category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class MyChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree, null);
    }
  }

  private ErrorProneCompiler compiler;
  private DiagnosticTestHelper diagnosticHelper;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(new ErrorProneScanner(new MyChecker()))
        .build();
  }

  @Test
  public void testPositiveCase() throws Exception {
    File source = new File(this.getClass().getResource("UnsuppressiblePositiveCases.java").toURI());
    assertThat(compiler.compile(new String[]{source.getAbsolutePath()}), is(1));
    assertThat(diagnosticHelper.getDiagnostics().toString(), containsString("[MyChecker]"));
  }
}
