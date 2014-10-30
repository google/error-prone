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
import static org.junit.Assert.assertThat;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.DiagnosticTestHelper;
import com.google.errorprone.ErrorProneTestCompiler;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.EmptyStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.main.Main.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import javax.tools.JavaFileObject;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CustomSuppressionTest {

  /**
   * Custom suppression annotation for the first checker in this test.
   */
  public @interface SuppressMyChecker{}

  @BugPattern(name = "MyChecker",
      summary = "Test checker that uses a custom suppression annotation",
      explanation = "Test checker that uses a custom suppression annotation",
      suppressibility = Suppressibility.CUSTOM_ANNOTATION,
      customSuppressionAnnotation = SuppressMyChecker.class,
      category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class MyChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  /**
   * Custom suppression annotation for the second checker in this test.
   */
  public @interface SuppressMyChecker2{}

  @BugPattern(name = "MyChecker2",
      summary = "Test checker that uses a different custom suppression annotation",
      explanation = "Test checker that uses a different custom suppression annotation",
      suppressibility = Suppressibility.CUSTOM_ANNOTATION,
      customSuppressionAnnotation = SuppressMyChecker2.class,
      category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class MyChecker2 extends BugChecker implements EmptyStatementTreeMatcher {
    @Override
    public Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  private ErrorProneTestCompiler compiler;
  private DiagnosticTestHelper diagnosticHelper;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    compiler = new ErrorProneTestCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(ScannerSupplier.fromBugCheckers(new MyChecker(), new MyChecker2()))
        .build();
  }

  @Test
  public void testNegativeCase() throws Exception {
    List<JavaFileObject> sources = compiler.fileManager()
        .sources(getClass(), "CustomSuppressionNegativeCases.java");
    Result exitCode = compiler.compile(sources);
    assertThat(exitCode, is(Result.OK));
  }

  @Test
  public void testPositiveCase() throws Exception {
    List<JavaFileObject> sources = compiler.fileManager()
        .sources(getClass(), "CustomSuppressionPositiveCases.java");
    assertThat(compiler.compile(sources), is(Result.ERROR));
    assertThat(diagnosticHelper.getDiagnostics().size(), is(3));
    assertThat((int) diagnosticHelper.getDiagnostics().get(0).getLineNumber(), is(28));
    assertThat((int) diagnosticHelper.getDiagnostics().get(1).getLineNumber(), is(33));
    assertThat((int) diagnosticHelper.getDiagnostics().get(2).getLineNumber(), is(38));
  }

}
