/*
 * Copyright 2011 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.NonAtomicVolatileUpdate;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.main.Main.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for the Error Prone compiler.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class ErrorProneCompilerIntegrationTest {

  private DiagnosticTestHelper diagnosticHelper;
  private StringWriter outputStream;
  private ErrorProneTestCompiler.Builder compilerBuilder;
  ErrorProneTestCompiler compiler;
  @Rule public final TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    outputStream = new StringWriter();
    compilerBuilder =
        new ErrorProneTestCompiler.Builder()
            .report(BuiltInCheckerSuppliers.defaultChecks())
            .redirectOutputTo(new PrintWriter(outputStream, true))
            .listenToDiagnostics(diagnosticHelper.collector);
    compiler = compilerBuilder.build();
  }

  @Test
  public void fileWithError() {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(BadShiftAmount.class, "testdata/BadShiftAmountPositiveCases.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[BadShiftAmount]")));
    assertTrue(
        "Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void fileWithWarning() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(NonAtomicVolatileUpdate.class));
    compiler = compilerBuilder.build();
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(
                    NonAtomicVolatileUpdate.class,
                    "testdata/NonAtomicVolatileUpdatePositiveCases.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[NonAtomicVolatileUpdate]")));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void fileWithMultipleTopLevelClasses() {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(getClass(), "testdata/MultipleTopLevelClassesWithNoErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @Test
  public void fileWithMultipleTopLevelClassesExtends() {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(
                    getClass(),
                    "testdata/MultipleTopLevelClassesWithNoErrors.java",
                    "testdata/ExtendedMultipleTopLevelClassesWithNoErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  /**
   * Regression test for a bug in which multiple top-level classes may cause NullPointerExceptions
   * in the matchers.
   */
  @Test
  public void fileWithMultipleTopLevelClassesExtendsWithError() {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(
                    getClass(),
                    "testdata/MultipleTopLevelClassesWithErrors.java",
                    "testdata/ExtendedMultipleTopLevelClassesWithErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[SelfAssignment]")));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
    assertThat(diagnosticHelper.getDiagnostics()).hasSize(4);
  }

  @BugPattern(name = "", explanation = "", summary = "", severity = ERROR, category = ONE_OFF)
  public static class Throwing extends BugChecker implements ExpressionStatementTreeMatcher {
    @Override
    public Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state) {
      throw new IllegalStateException("test123");
    }
  }

  @Test
  public void unhandledExceptionsAreReportedWithoutBugParadeLink() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(Throwing.class));
    compiler = compilerBuilder.build();
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(
                    getClass(),
                    "testdata/MultipleTopLevelClassesWithErrors.java",
                    "testdata/ExtendedMultipleTopLevelClassesWithErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(
            diagnosticMessage(
                CoreMatchers.<String>allOf(
                    containsString("IllegalStateException: test123"),
                    containsString("unhandled exception was thrown by the Error Prone"))));
    assertTrue(
        "Error should be reported. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  /** Regression test for Issue 188, error-prone doesn't work with annotation processors. */
  @Test
  public void annotationProcessingWorks() {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(getClass(), "testdata/UsesAnnotationProcessor.java"),
            Arrays.asList(new NullAnnotationProcessor()));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  /** Test that if javac does dataflow on a class twice error-prone only analyses it once. */
  @Test
  public void reportReadyForAnalysisOnce() {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(
                    getClass(),
                    "testdata/FlowConstants.java",
                    "testdata/FlowSub.java",
                    // This order is important: the superclass needs to occur after the subclass in
                    // the sources so it goes through flow twice (once so it can be used when the
                    // subclass is desugared, once normally).
                    "testdata/FlowSuper.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @BugPattern(
      name = "ConstructorMatcher",
      explanation = "",
      category = ONE_OFF,
      severity = ERROR,
      summary = "")
  public static class ConstructorMatcher extends BugChecker implements MethodTreeMatcher {
    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void ignoreGeneratedConstructors() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ConstructorMatcher.class));
    compiler = compilerBuilder.build();
    Result exitCode =
        compiler.compile(
            Arrays.asList(
                compiler.fileManager().forSourceLines("Test.java", "public class Test {}")));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        not(hasItem(diagnosticMessage(containsString("[ConstructorMatcher]"))));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @BugPattern(
      name = "SuperCallMatcher",
      explanation = "",
      category = ONE_OFF,
      severity = ERROR,
      summary = "")
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
        return NO_MATCH;
      }
      return name.contentEquals("super") ? describeMatch(tree) : Description.NO_MATCH;
    }
  }

  // TODO(cushon) - how can we distinguish between synthetic super() calls and real ones?
  @Ignore
  @Test
  public void ignoreGeneratedSuperInvocations() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(SuperCallMatcher.class));
    compiler = compilerBuilder.build();
    Result exitCode =
        compiler.compile(
            Arrays.asList(
                compiler
                    .fileManager()
                    .forSourceLines(
                        "Test.java", "public class Test {", "  public Test() {}", "}")));

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        not(hasItem(diagnosticMessage(containsString("[SuperCallMatcher]"))));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @Test
  public void invalidFlagCausesCmdErrResult() {
    String[] args = {"-Xep:"};
    assertThrows(
        InvalidCommandLineOptionException.class,
        () ->
            compiler.compile(
                args,
                Arrays.asList(
                    compiler
                        .fileManager()
                        .forSourceLines(
                            "Test.java", //
                            "public class Test {",
                            "  public Test() {}",
                            "}"))));
  }

  @Test
  public void flagEnablesCheck() {
    String[] testFile = {"public class Test {", "  public Test() {", "    if (true);", "  }", "}"};

    Result exitCode =
        compiler.compile(
            Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
    assertThat(outputStream.toString(), exitCode, is(Result.OK));

    String[] args = {"-Xep:EmptyIf"};
    exitCode =
        compiler.compile(
            args, Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();

    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[EmptyIf]")));
    assertTrue(
        "Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
  }

  @Test
  public void severityIsResetOnNextCompilation() {
    String[] testFile = {"public class Test {", "  void doIt (int i) {", "    i = i;", "  }", "}"};

    String[] args = {"-Xep:SelfAssignment:WARN"};
    Result exitCode =
        compiler.compile(
            args, Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[SelfAssignment]")));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
    assertTrue(
        "Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    // Should reset to default severity (ERROR)
    exitCode =
        compiler.compile(
            Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
  }

  @Test
  public void maturityIsResetOnNextCompilation() {
    String[] testFile = {"public class Test {", "  public Test() {", "    if (true);", "  }", "}"};

    String[] args = {"-Xep:EmptyIf"};
    Result exitCode =
        compiler.compile(
            args, Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[EmptyIf]")));
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
    assertTrue(
        "Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));

    diagnosticHelper.clearDiagnostics();
    exitCode =
        compiler.compile(
            Arrays.asList(compiler.fileManager().forSourceLines("Test.java", testFile)));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @Test
  public void suppressGeneratedWarning() {
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
      assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(ENGLISH))
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
  public void suppressGeneratedWarningJava9() {
    assumeTrue(StandardSystemProperty.JAVA_VERSION.value().startsWith("9"));
    String[] generatedFile = {
      "@javax.annotation.processing.Generated(\"Foo\")",
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
      assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(ENGLISH))
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
  public void cannotSuppressGeneratedError() {
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
    assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(ENGLISH)).contains("[EmptyIf]");
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));
  }

  @BugPattern(
      name = "CrashOnReturn",
      explanation = "",
      summary = "",
      severity = ERROR,
      category = ONE_OFF)
  public static class CrashOnReturn extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      throw new NullPointerException();
    }
  }

  @Test
  public void crashSourcePosition() {
    compiler =
        compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(CrashOnReturn.class)).build();
    Result exitCode =
        compiler.compile(
            Arrays.asList(
                compiler
                    .fileManager()
                    .forSourceLines(
                        "test/Test.java",
                        "package Test;",
                        "class Test {",
                        "  void f() {",
                        "    return;",
                        "  }",
                        "}")));
    assertThat(exitCode).named(outputStream.toString()).isEqualTo(Result.ERROR);
    assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
    Diagnostic<? extends JavaFileObject> diag =
        Iterables.getOnlyElement(diagnosticHelper.getDiagnostics());
    assertThat(diag.getLineNumber()).isEqualTo(4);
    assertThat(diag.getColumnNumber()).isEqualTo(5);
    assertThat(diag.getSource().toUri().toString()).endsWith("test/Test.java");
    assertThat(diag.getMessage(ENGLISH))
        .contains("An unhandled exception was thrown by the Error Prone static analysis plugin");
  }

  @Test
  public void compilePolicy_bytodo() {
    InvalidCommandLineOptionException e =
        assertThrows(
            InvalidCommandLineOptionException.class,
            () ->
                compiler.compile(
                    new String[] {"-XDcompilePolicy=bytodo"},
                    Collections.<JavaFileObject>emptyList()));
    assertThat(e).hasMessageThat().contains("-XDcompilePolicy=bytodo is not supported");
  }

  @Test
  public void compilePolicy_byfile() {
    Result exitCode =
        compiler.compile(
            new String[] {"-XDcompilePolicy=byfile"},
            Arrays.asList(compiler.fileManager().forSourceLines("Test.java", "class Test {}")));
    outputStream.flush();
    assertThat(exitCode).named(outputStream.toString()).isEqualTo(Result.OK);
  }

  @Test
  public void compilePolicy_simple() {
    Result exitCode =
        compiler.compile(
            new String[] {"-XDcompilePolicy=simple"},
            Arrays.asList(compiler.fileManager().forSourceLines("Test.java", "class Test {}")));
    outputStream.flush();
    assertThat(exitCode).named(outputStream.toString()).isEqualTo(Result.OK);
  }

  @BugPattern(
      name = "CPSChecker",
      summary = "Using 'return' is considered harmful",
      explanation = "Please refactor your code into continuation passing style.",
      category = ONE_OFF,
      severity = ERROR)
  public static class CPSChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void compilationWithError() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(CPSChecker.class));
    compiler = compilerBuilder.build();
    compiler.compile(
        new String[] {
          "-XDshouldStopPolicyIfError=LOWER",
        },
        Arrays.asList(
            compiler
                .fileManager()
                .forSourceLines(
                    "Test.java",
                    "package test;",
                    "public class Test {",
                    "  Object f() { return new NoSuch(); }",
                    "}")));
    outputStream.flush();
    String output = diagnosticHelper.getDiagnostics().toString();
    assertThat(output).contains("error: cannot find symbol");
    assertThat(output).doesNotContain("Using 'return' is considered harmful");
  }


  /**
   * Trivial bug checker for testing command line flags. Forbids methods from returning the string
   * provided by "-XepOpt:Forbidden=<VALUE>" flag.
   */
  @BugPattern(
      name = "ForbiddenString",
      summary = "Please don't return this const value",
      category = ONE_OFF,
      severity = ERROR)
  public static class ForbiddenString extends BugChecker implements ReturnTreeMatcher {
    private final String forbiddenString;

    public ForbiddenString(ErrorProneFlags flags) {
      forbiddenString = flags.get("Forbidden").orElse("default");
    }

    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (this.forbiddenString.equalsIgnoreCase(constValue(tree.getExpression()).toString())) {
        return describeMatch(tree);
      } else {
        return NO_MATCH;
      }
    }
  }

  @Test
  public void checkerWithFlags() {
    String[] args = {
      "-XepOpt:Forbidden=xylophone",
    };
    List<JavaFileObject> sources =
        Arrays.asList(
            compiler
                .fileManager()
                .forSourceLines(
                    "Test.java",
                    "package test;",
                    "public class Test {",
                    "  Object f() { return \"XYLOPHONE\"; }",
                    "}"));

    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ForbiddenString.class));
    compiler = compilerBuilder.build();
    compiler.compile(args, sources);
    outputStream.flush();
    String output = diagnosticHelper.getDiagnostics().toString();
    assertThat(output).contains("Please don't return this const value");
  }

  @Test
  public void flagsAreResetOnNextCompilation() {
    String[] args = {"-XepOpt:Forbidden=bananas"};
    List<JavaFileObject> sources =
        Arrays.asList(
            compiler
                .fileManager()
                .forSourceLines(
                    "Test.java",
                    "package test;",
                    "public class Test {",
                    "  Object f() { return \"BANANAS\"; }",
                    "}"));

    // First compile forbids "bananas", should fail.
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ForbiddenString.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(args, sources);
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(Result.ERROR));

    // Flags should reset, compile should succeed.
    exitCode = compiler.compile(sources);
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }
}
