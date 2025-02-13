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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.DiagnosticTestHelper.DIAGNOSTIC_CONTAINING;
import static com.google.errorprone.FileObjects.forResources;
import static com.google.errorprone.FileObjects.forSourceLines;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import com.google.common.base.Ascii;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.truth.Correspondence;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.NonAtomicVolatileUpdate;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.main.Main.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
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
            forResources(BadShiftAmount.class, "testdata/BadShiftAmountPositiveCases.java"));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);

    assertWithMessage("Error should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[BadShiftAmount]");
  }

  @Test
  public void fileWithWarning() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(NonAtomicVolatileUpdate.class));
    compiler = compilerBuilder.build();
    Result exitCode =
        compiler.compile(
            forResources(
                NonAtomicVolatileUpdate.class,
                "testdata/NonAtomicVolatileUpdatePositiveCases.java"));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);

    assertWithMessage("Warning should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[NonAtomicVolatileUpdate]");
  }

  @Test
  public void fileWithMultipleTopLevelClasses() {
    Result exitCode =
        compiler.compile(
            forResources(getClass(), "testdata/MultipleTopLevelClassesWithNoErrors.java"));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @Test
  public void fileWithMultipleTopLevelClassesExtends() {
    Result exitCode =
        compiler.compile(
            forResources(
                getClass(),
                "testdata/MultipleTopLevelClassesWithNoErrors.java",
                "testdata/ExtendedMultipleTopLevelClassesWithNoErrors.java"));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  /**
   * Regression test for a bug in which multiple top-level classes may cause NullPointerExceptions
   * in the matchers.
   */
  @Test
  public void fileWithMultipleTopLevelClassesExtendsWithError() {
    Result exitCode =
        compiler.compile(
            forResources(
                getClass(),
                "testdata/MultipleTopLevelClassesWithErrors.java",
                "testdata/ExtendedMultipleTopLevelClassesWithErrors.java"));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
    assertWithMessage("Warning should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[SelfAssignment]");
    assertThat(diagnosticHelper.getDiagnostics()).hasSize(4);
  }

  @BugPattern(summary = "", severity = ERROR)
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
            forResources(
                getClass(),
                "testdata/MultipleTopLevelClassesWithErrors.java",
                "testdata/ExtendedMultipleTopLevelClassesWithErrors.java"));

    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
    assertWithMessage("Error should be reported. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(
            Correspondence.<Diagnostic<? extends JavaFileObject>, List<String>>from(
                (diagnostic, messages) ->
                    messages.stream()
                        .allMatch(m -> diagnostic.getMessage(Locale.getDefault()).contains(m)),
                "diagnostic containing"))
        .contains(
            ImmutableList.of(
                "IllegalStateException: test123",
                "unhandled exception was thrown by the Error Prone"));
  }

  /** Regression test for Issue 188, error-prone doesn't work with annotation processors. */
  @Test
  public void annotationProcessingWorks() {
    Result exitCode =
        compiler.compile(
            forResources(getClass(), "testdata/UsesAnnotationProcessor.java"),
            Arrays.asList(new NullAnnotationProcessor()));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  /** Test that if javac does dataflow on a class twice error-prone only analyses it once. */
  @Test
  public void reportReadyForAnalysisOnce() {
    Result exitCode =
        compiler.compile(
            forResources(
                getClass(),
                "testdata/FlowConstants.java",
                "testdata/FlowSub.java",
                // This order is important: the superclass needs to occur after the subclass in
                // the sources so it goes through flow twice (once so it can be used when the
                // subclass is desugared, once normally).
                "testdata/FlowSuper.java"));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @BugPattern(severity = ERROR, summary = "")
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
                forSourceLines(
                    "Test.java", //
                    "public class Test {}")));

    assertWithMessage("Warning should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .doesNotContain("[ConstructorMatcher]");

    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @BugPattern(explanation = "", severity = ERROR, summary = "")
  static class SuperCallMatcher extends BugChecker implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      Tree select = tree.getMethodSelect();
      Name name;
      if (select instanceof MemberSelectTree memberSelectTree) {
        name = memberSelectTree.getIdentifier();
      } else if (select instanceof IdentifierTree identifierTree) {
        name = identifierTree.getName();
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
                forSourceLines(
                    "Test.java",
                    """
                    public class Test {
                      public Test() {}
                    }
                    """)));

    assertWithMessage("[SuperCallMatcher]")
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .doesNotContain("Warning should be found. " + diagnosticHelper.describe());

    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
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
                    forSourceLines(
                        "Test.java",
                        """
                        public class Test {
                          public Test() {}
                        }
                        """))));
  }

  @Test
  public void flagEnablesCheck() {
    String[] testFile = {
      "package test;", //
      "public class Test {",
      "  public Test() {",
      "    if (true);",
      "  }",
      "}"
    };
    List<JavaFileObject> fileObjects = Arrays.asList(forSourceLines("Test.java", testFile));
    Result exitCode = compiler.compile(fileObjects);
    outputStream.flush();
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);

    String[] args = {"-Xep:EmptyIf"};
    exitCode = compiler.compile(args, fileObjects);
    outputStream.flush();

    assertWithMessage("Error should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[EmptyIf]");

    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
  }

  @Test
  public void severityIsResetOnNextCompilation() {
    String[] testFile = {
      "package test;", //
      "public class Test {",
      "  void doIt (int i) {",
      "    i = i;",
      "  }",
      "}"
    };
    List<JavaFileObject> fileObjects = Arrays.asList(forSourceLines("Test.java", testFile));
    String[] args = {"-Xep:SelfAssignment:WARN"};
    Result exitCode = compiler.compile(args, fileObjects);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
    assertWithMessage("Warning should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[SelfAssignment]");

    // Should reset to default severity (ERROR)
    exitCode = compiler.compile(fileObjects);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
  }

  @Test
  public void maturityIsResetOnNextCompilation() {
    String[] testFile = {
      "package test;", //
      "public class Test {",
      "  public Test() {",
      "    if (true);",
      "  }",
      "}"
    };
    List<JavaFileObject> fileObjects = Arrays.asList(forSourceLines("Test.java", testFile));
    String[] args = {"-Xep:EmptyIf"};
    Result exitCode = compiler.compile(args, fileObjects);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
    assertWithMessage("Error should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .contains("[EmptyIf]");

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(fileObjects);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @Test
  public void suppressGeneratedWarning() {
    String[] generatedFile = {
      "import java.util.List;",
      "@javax.annotation.Generated(\"Foo\")",
      "class Generated {",
      "  public Generated() {}",
      "}"
    };

    List<JavaFileObject> fileObjects =
        Arrays.asList(forSourceLines("Generated.java", generatedFile));

    {
      String[] args = {"-Xep:RemoveUnusedImports:WARN"};
      Result exitCode = compiler.compile(args, fileObjects);
      outputStream.flush();
      assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
      assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(ENGLISH))
          .contains("[RemoveUnusedImports]");
      assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
    }

    diagnosticHelper.clearDiagnostics();

    {
      String[] args = {"-Xep:RemoveUnusedImports:WARN", "-XepDisableWarningsInGeneratedCode"};
      Result exitCode = compiler.compile(args, fileObjects);
      outputStream.flush();
      assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
      assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
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

    List<JavaFileObject> fileObjects =
        Arrays.asList(forSourceLines("Generated.java", generatedFile));
    {
      String[] args = {"-Xep:EmptyIf:WARN"};
      Result exitCode = compiler.compile(args, fileObjects);
      outputStream.flush();
      assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
      assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(ENGLISH))
          .contains("[EmptyIf]");
      assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
    }

    diagnosticHelper.clearDiagnostics();

    {
      String[] args = {"-Xep:EmptyIf:WARN", "-XepDisableWarningsInGeneratedCode"};
      Result exitCode = compiler.compile(args, fileObjects);
      outputStream.flush();
      assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
      assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
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
        compiler.compile(args, Arrays.asList(forSourceLines("Generated.java", generatedFile)));
    outputStream.flush();
    assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(diagnosticHelper.getDiagnostics().get(0).getMessage(ENGLISH)).contains("[EmptyIf]");
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
  }

  @BugPattern(explanation = "", summary = "", severity = ERROR)
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
                forSourceLines(
                    "test/Test.java",
                    """
                    package Test;
                    class Test {
                      void f() {
                        return;
                      }
                    }
                    """)));
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
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
            Arrays.asList(
                forSourceLines(
                    "Test.java",
                    """
                    package test;
                    class Test {}
                    """)));
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @Test
  public void compilePolicy_simple() {
    Result exitCode =
        compiler.compile(
            new String[] {"-XDcompilePolicy=simple"},
            Arrays.asList(
                forSourceLines(
                    "Test.java",
                    """
                    package test;
                    class Test {}
                    """)));
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @BugPattern(
      summary = "Using 'return' is considered harmful",
      explanation = "Please refactor your code into continuation passing style.",
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
    Result exitCode =
        compiler.compile(
            Arrays.asList(
                forSourceLines(
                    "Test.java",
                    """
                    package test;
                    public class Test {
                      Object f() { return new NoSuch(); }
                    }
                    """)));
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
    String output = diagnosticHelper.getDiagnostics().toString();
    assertThat(output).contains("error: cannot find symbol");
    assertThat(output).doesNotContain("Using 'return' is considered harmful");
  }

  /**
   * Trivial bug checker for testing command line flags. Forbids methods from returning the string
   * provided by "-XepOpt:Forbidden=<VALUE>" flag.
   */
  @BugPattern(summary = "Please don't return this const value", severity = ERROR)
  public static class ForbiddenString extends BugChecker implements ReturnTreeMatcher {
    private final String forbiddenString;

    @Inject
    ForbiddenString(ErrorProneFlags flags) {
      forbiddenString = flags.get("Forbidden").orElse("default");
    }

    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (Ascii.equalsIgnoreCase(
          this.forbiddenString, constValue(tree.getExpression()).toString())) {
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
            forSourceLines(
                "Test.java",
                """
                package test;
                public class Test {
                  Object f() { return "XYLOPHONE"; }
                }
                """));

    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ForbiddenString.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(args, sources);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
    String output = diagnosticHelper.getDiagnostics().toString();
    assertThat(output).contains("Please don't return this const value");
  }

  @Test
  public void flagsAreResetOnNextCompilation() {
    String[] args = {"-XepOpt:Forbidden=bananas"};
    List<JavaFileObject> sources =
        Arrays.asList(
            forSourceLines(
                "Test.java",
                """
                package test;
                public class Test {
                  Object f() { return "BANANAS"; }
                }
                """));

    // First compile forbids "bananas", should fail.
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ForbiddenString.class));
    compiler = compilerBuilder.build();
    Result exitCode = compiler.compile(args, sources);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);

    // Flags should reset, compile should succeed.
    exitCode = compiler.compile(sources);
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @BugPattern(summary = "All variables should be effectively final", severity = ERROR)
  public static class EffectivelyFinalChecker extends BugChecker implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      if (ASTHelpers.isConsideredFinal(ASTHelpers.getSymbol(tree))) {
        return NO_MATCH;
      }
      return describeMatch(tree);
    }
  }

  @Test
  public void stopPolicy_effectivelyFinal() {
    compilerBuilder.report(
        ScannerSupplier.fromBugCheckerClasses(EffectivelyFinalChecker.class, CPSChecker.class));
    compiler = compilerBuilder.build();
    // Without --should-stop=ifError=FLOW, the errors reported by CPSChecker will cause javac to
    // stop processing B after an error is reported in A. Error Prone will still analyze B without
    // it having gone through 'flow', and the EFFECTIVELY_FINAL analysis will not have happened.
    // see https://github.com/google/error-prone/issues/4595
    Result exitCode =
        compiler.compile(
            ImmutableList.of(
                forSourceLines(
                    "A.java",
                    """
                    class A {
                      int f(int x) {
                        return x;
                      }
                    }
                    """),
                forSourceLines(
                    "B.java",
                    """
                    class B {
                      int f(int x) {
                        return x;
                      }
                    }
                    """)));

    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);

    assertThat(diagnosticHelper.getDiagnostics()).hasSize(2);
    assertWithMessage("Error should be found. " + diagnosticHelper.describe())
        .that(diagnosticHelper.getDiagnostics())
        .comparingElementsUsing(DIAGNOSTIC_CONTAINING)
        .containsExactly("[CPSChecker]", "[CPSChecker]");
  }

  @Test
  public void stopPolicy_flow() {
    Result exitCode =
        compiler.compile(
            new String[] {"--should-stop=ifError=FLOW"},
            ImmutableList.of(
                forSourceLines(
                    "Test.java",
                    """
                    package test;
                    class Test {}
                    """)));
    outputStream.flush();
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.OK);
  }

  @Test
  public void stopPolicy_init() {
    InvalidCommandLineOptionException e =
        assertThrows(
            InvalidCommandLineOptionException.class,
            () ->
                compiler.compile(new String[] {"--should-stop=ifError=INIT"}, ImmutableList.of()));
    assertThat(e).hasMessageThat().contains("--should-stop=ifError=INIT is not supported");
  }

  @Test
  public void stopPolicy_init_xD() {
    InvalidCommandLineOptionException e =
        assertThrows(
            InvalidCommandLineOptionException.class,
            () ->
                compiler.compile(new String[] {"-XDshould-stop.ifError=INIT"}, ImmutableList.of()));
    assertThat(e).hasMessageThat().contains("--should-stop=ifError=INIT is not supported");
  }

  @BugPattern(summary = "Checks that symbols are non-null", severity = ERROR)
  public static class GetSymbolChecker extends BugChecker implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      requireNonNull(ASTHelpers.getSymbol(tree));
      return NO_MATCH;
    }
  }

  @BugPattern(summary = "Duplicates CPSChecker", severity = ERROR)
  public static class CPSChecker2 extends CPSChecker {}

  // This is a regression test for a crash if two Error Prone diagnostics are reported at the same
  // position. javac has logic to filter duplicate diagnostics, which can result in counts of
  // Error Prone and other diagnostics getting out of sync with the counters kept in
  // ErrorProneAnalyzer, which are used to skip Error Prone checks on compilation units with
  // underlying errors. If the checks run on compilations with underlying errors they may see
  // null symbols.
  @Test
  public void processingError() {
    compilerBuilder.report(
        ScannerSupplier.fromBugCheckerClasses(
            CPSChecker.class, CPSChecker2.class, GetSymbolChecker.class));
    compiler = compilerBuilder.build();
    Result exitCode =
        compiler.compile(
            new String[] {"--should-stop=ifError=FLOW", "-XDcompilePolicy=byfile"},
            ImmutableList.of(
                forSourceLines(
                    "A.java",
                    """
                    class A {
                      int f(int x) {
                        return x;
                      }
                    }
                    """),
                forSourceLines(
                    "B.java",
                    """
                    package test;
                    import java.util.HashSet;
                    import java.util.Set;
                    class B {
                      enum E { ONE }
                      E f(int s) {
                        return E.valueOf(s);
                      }
                    }
                    """)),
            ImmutableList.of());
    assertWithMessage(outputStream.toString()).that(exitCode).isEqualTo(Result.ERROR);
    ImmutableList<String> diagnostics =
        diagnosticHelper.getDiagnostics().stream()
            .filter(d -> d.getKind().equals(Diagnostic.Kind.ERROR))
            .map(d -> d.getCode())
            .collect(toImmutableList());
    assertThat(diagnostics).doesNotContain("compiler.err.error.prone.crash");
    assertThat(diagnostics).hasSize(3);
  }
}
