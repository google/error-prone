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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.NonAtomicVolatileUpdate;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Integration tests for {@link ErrorProneCompiler}.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class ErrorProneCompilerIntegrationTest {

  private DiagnosticTestHelper diagnosticHelper;
  private StringWriter outputStream;
  private ErrorProneTestCompiler.Builder compilerBuilder;
  ErrorProneTestCompiler compiler;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    outputStream = new StringWriter();
    compilerBuilder = new ErrorProneTestCompiler.Builder()
        .named("test")
        .redirectOutputTo(new PrintWriter(outputStream, true))
        .listenToDiagnostics(diagnosticHelper.collector);
    compiler = compilerBuilder.build();
  }

  @Test
  public void fileWithError() throws Exception {
    Result exitCode = compiler.compile(compiler.fileManager().forResources(
        BadShiftAmount.class,
        "BadShiftAmountPositiveCases.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[BadShiftAmount]")));
    assertTrue("Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void fileWithWarning() throws Exception {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(NonAtomicVolatileUpdate.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(compiler.fileManager().forResources(
        NonAtomicVolatileUpdate.class,
        "NonAtomicVolatileUpdatePositiveCases.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[NonAtomicVolatileUpdate]")));
    assertTrue("Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void fileWithMultipleTopLevelClasses() throws Exception {
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(),
            "MultipleTopLevelClassesWithNoErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @Test
  public void fileWithMultipleTopLevelClassesExtends() throws Exception {
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(), "MultipleTopLevelClassesWithNoErrors.java",
            "ExtendedMultipleTopLevelClassesWithNoErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  /**
   * Regression test for a bug in which multiple top-level classes may cause
   * NullPointerExceptions in the matchers.
   */
  @Test
  public void fileWithMultipleTopLevelClassesExtendsWithError()
      throws Exception {
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(), "MultipleTopLevelClassesWithErrors.java",
            "ExtendedMultipleTopLevelClassesWithErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[SelfAssignment]")));
    assertTrue("Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
    assertThat(diagnosticHelper.getDiagnostics()).hasSize(4);
  }

  @BugPattern(
      name = "", explanation = "", summary = "", maturity = EXPERIMENTAL, severity = ERROR,
      category = ONE_OFF)
  public static class Throwing extends BugChecker implements ExpressionStatementTreeMatcher {
    @Override
    public Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state) {
      throw new IllegalStateException("test123");
    }
  }

  @Test
  public void unhandledExceptionsAreReportedWithoutBugParadeLink() throws Exception {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(Throwing.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(), "MultipleTopLevelClassesWithErrors.java",
            "ExtendedMultipleTopLevelClassesWithErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(CoreMatchers.<String>allOf(
            containsString("IllegalStateException: test123"),
            containsString("unhandled exception was thrown by the Error Prone"))));
    assertTrue("Error should be reported. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  /**
   * Regression test for Issue 188, error-prone doesn't work with annotation processors.
   */
  @Test
  public void annotationProcessingWorks() throws Exception {
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(), "UsesAnnotationProcessor.java"),
        List.of(new NullAnnotationProcessor()));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  /**
   * Test that if javac does dataflow on a class twice error-prone only analyses it once.
   */
  @Test
  public void reportReadyForAnalysisOnce() throws Exception {
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(),
            "FlowConstants.java",
            "FlowSub.java",
            // This order is important: the superclass needs to occur after the subclass in the
            // sources so it goes through flow twice (once so it can be used when the subclass
            // is desugared, once normally).
            "FlowSuper.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @Test
  public void propagatesScannerThroughAnnotationProcessingRounds() throws Exception {
    final ErrorProneScanner scanner =
        new ErrorProneScanner(
            Collections.<BugChecker>emptyList(), Collections.<String, SeverityLevel>emptyMap());
    compilerBuilder.report(ScannerSupplier.fromScanner(scanner));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(
        compiler.fileManager().forResources(getClass(), "UsesAnnotationProcessor.java"),
        Arrays.asList(new ScannerCheckingProcessor(scanner)));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  /**
   * Annotation processor that checks that the context always has the same {@link ErrorProneScanner}
   * instance at each stage of annotation processing.
   */
  @SupportedAnnotationTypes("*")
  public static final class ScannerCheckingProcessor extends AbstractProcessor {

    private final ErrorProneScanner expected;

    public ScannerCheckingProcessor(ErrorProneScanner expected) {
      this.expected = expected;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
      assertSame(expected, context.get(Scanner.class));
      return false;
    }
  }

  @BugPattern(name = "ConstructorMatcher", explanation = "",
      category = ONE_OFF, maturity = EXPERIMENTAL, severity = ERROR, summary = "")
  public static class ConstructorMatcher extends BugChecker implements MethodTreeMatcher {
    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void ignoreGeneratedConstructors() throws Exception {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ConstructorMatcher.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java", "public class Test {}")));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = not(hasItem(
        diagnosticMessage(containsString("[ConstructorMatcher]"))));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @BugPattern(name = "SuperCallMatcher", explanation = "",
      category = ONE_OFF, maturity = EXPERIMENTAL, severity = ERROR, summary = "")
  static class SuperCallMatcher extends BugChecker implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      Tree select = tree.getMethodSelect();
      Name name;
      if (select instanceof MemberSelectTree) {
        name = ((MemberSelectTree) select).getIdentifier();
      } else if (select instanceof IdentifierTree) {
        name = ((IdentifierTree) select).getName();
      } else {
        return Description.NO_MATCH;
      }
      return name.contentEquals("super")
          ? describeMatch(tree)
          : Description.NO_MATCH;
    }
  }

  // TODO(cushon) - how can we distinguish between synthetic super() calls and real ones?
  @Ignore
  @Test
  public void ignoreGeneratedSuperInvocations() throws Exception {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(SuperCallMatcher.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(Arrays.asList(
        compiler.fileManager().forSourceLines("Test.java",
            "public class Test {",
            "  public Test() {}",
            "}")));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = not(hasItem(
        diagnosticMessage(containsString("[SuperCallMatcher]"))));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @Test
  public void invalidFlagCausesCmdErrResult() throws Exception {
    String[] args = {"-Xep:"};
    Result exitCode = compiler.compile(args,
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java",
            "public class Test {",
            "  public Test() {}",
            "}")));
    outputStream.flush();

    assertThat(outputStream.toString(), exitCode, is(Result.CMDERR));
  }

  @Test
  public void flagEnablesCheck() throws Exception {
    String[] testFile = {"public class Test {",
        "  public Test() {",
        "    if (true);",
        "  }",
        "}"};

    Result exitCode = compiler.compile(Arrays.asList(
        compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
    assertThat(outputStream.toString(), exitCode, is(Result.OK));

    String[] args = {"-Xep:EmptyIf"};
    exitCode = compiler.compile(args,
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[EmptyIf]")));
    assertTrue(
        "Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
  }

  @Test
  public void severityIsResetOnNextCompilation() throws Exception {
    String[] testFile = {"public class Test {",
        "  long myLong = 213124l;",
        "}"};

    String[] args = {"-Xep:LongLiteralLowerCaseSuffix:WARN"};
    Result exitCode = compiler.compile(args,
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[LongLiteralLowerCaseSuffix]")));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    // Should reset to default severity (ERROR)
    exitCode = compiler.compile(
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
  }

  @Test
  public void maturityIsResetOnNextCompilation() throws Exception {
    String[] testFile = {"public class Test {",
        "  public Test() {",
        "    if (true);",
        "  }",
        "}"};

    String[] args = {"-Xep:EmptyIf"};
    Result exitCode = compiler.compile(args,
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[EmptyIf]")));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
    assertTrue(
        "Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(
        Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }
  
  @Test
  public void suppressGeneratedWarning() throws Exception {
    String[] generatedFile = {
      "@javax.annotation.Generated(\"Foo\")",
      "class Generated {",
      "  public Generated() {",
      "    if (true);",
      "  }",
      "}"
    };

    {
      String[] args = {"-Xep:EmptyIf:WARN"};
      Result exitCode =
          compiler.compile(
              args,
              Arrays.asList(
                  compiler.fileManager().forSourceLines("Generated.java", generatedFile)));
      outputStream.flush();
      assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
      assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(Locale.ENGLISH))
          .contains("[EmptyIf]");
      assertThat(outputStream.toString(), exitCode, is(Result.OK));
    }

    diagnosticHelper.clearDiagnostics();

    {
      String[] args = {"-Xep:EmptyIf:WARN", "-XepDisableWarningsInGeneratedCode"};
      Result exitCode =
          compiler.compile(
              args,
              Arrays.asList(
                  compiler.fileManager().forSourceLines("Generated.java", generatedFile)));
      outputStream.flush();
      assertThat(diagnosticHelper.getDiagnostics()).hasSize(0);
      assertThat(outputStream.toString(), exitCode, is(Result.OK));
    }
  }

  @Test
  public void cannotSuppressGeneratedError() throws Exception {
    String[] generatedFile = {
      "@javax.annotation.Generated(\"Foo\")",
      "class Generated {",
      "  public Generated() {",
      "    if (true);",
      "  }",
      "}"
    };

    String[] args = {"-Xep:EmptyIf:ERROR", "-XepDisableWarningsInGeneratedCode"};
    Result exitCode =
        compiler.compile(
            args,
            Arrays.asList(compiler.fileManager().forSourceLines("Generated.java", generatedFile)));
    outputStream.flush();
    assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(Locale.ENGLISH))
        .contains("[EmptyIf]");
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
  }
}
