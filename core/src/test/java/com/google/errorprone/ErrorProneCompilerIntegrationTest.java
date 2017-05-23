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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
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
  public void fileWithError() throws Exception {
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
  public void fileWithWarning() throws Exception {
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
  public void fileWithMultipleTopLevelClasses() throws Exception {
    Result exitCode =
        compiler.compile(
            compiler
                .fileManager()
                .forResources(getClass(), "testdata/MultipleTopLevelClassesWithNoErrors.java"));
    assertThat(outputStream.toString(), exitCode, is(Result.OK));
  }

  @Test
  public void fileWithMultipleTopLevelClassesExtends() throws Exception {
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
  public void fileWithMultipleTopLevelClassesExtendsWithError() throws Exception {
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
  public void unhandledExceptionsAreReportedWithoutBugParadeLink() throws Exception {
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
  public void annotationProcessingWorks() throws Exception {
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
  public void reportReadyForAnalysisOnce() throws Exception {
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
    summary = ""
  )
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
    summary = ""
  )
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
  public void ignoreGeneratedSuperInvocations() throws Exception {
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
  public void invalidFlagCausesCmdErrResult() throws Exception {
    String[] args = {"-Xep:"};
    Result exitCode =
        compiler.compile(
            args,
            Arrays.asList(
                compiler
                    .fileManager()
                    .forSourceLines(
                        "Test.java", "public class Test {", "  public Test() {}", "}")));
    outputStream.flush();

    assertThat(outputStream.toString(), exitCode, is(Result.CMDERR));
  }

  @Test
  public void flagEnablesCheck() throws Exception {
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
  public void severityIsResetOnNextCompilation() throws Exception {
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
  public void maturityIsResetOnNextCompilation() throws Exception {
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

  @BugPattern(
    name = "CrashOnReturn",
    explanation = "",
    summary = "",
    severity = ERROR,
    category = ONE_OFF
  )
  public static class CrashOnReturn extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      throw new NullPointerException();
    }
  }

  @Test
  public void crashSourcePosition() throws Exception {
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
    assertThat(diag.getMessage(Locale.ENGLISH))
        .contains("An unhandled exception was thrown by the Error Prone static analysis plugin");
  }

  @Test
  public void compilePolicy_bytodo() throws Exception {
    Result exitCode =
        compiler.compile(
            new String[] {"-XDcompilePolicy=bytodo"}, Collections.<JavaFileObject>emptyList());
    outputStream.flush();
    assertThat(exitCode).named(outputStream.toString()).isEqualTo(Result.CMDERR);
    assertThat(outputStream.toString()).contains("-XDcompilePolicy=bytodo is not supported");
  }

  @Test
  public void compilePolicy_byfile() throws Exception {
    Result exitCode =
        compiler.compile(
            new String[] {"-XDcompilePolicy=byfile"},
            Arrays.asList(compiler.fileManager().forSourceLines("Test.java", "class Test {}")));
    outputStream.flush();
    assertThat(exitCode).named(outputStream.toString()).isEqualTo(Result.OK);
  }

  @Test
  public void compilePolicy_simple() throws Exception {
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
    severity = ERROR
  )
  public static class CPSChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void compilationWithError() throws Exception {
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

  @Test
  public void plugin() throws Exception {

    Path base = tmpFolder.newFolder().toPath();
    Path source = base.resolve("test/Test.java");
    Files.createDirectories(source.getParent());
    Files.write(
        source,
        Arrays.asList(
            "package test;", //
            "public class Test {",
            "  int f() { return 42; }",
            "}"),
        UTF_8);

    Path jar = base.resolve("libproc.jar");
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
      jos.putNextEntry(new JarEntry("META-INF/services/" + BugChecker.class.getName()));
      jos.write((CPSChecker.class.getName() + "\n").getBytes(UTF_8));
      String classFile = CPSChecker.class.getName().replace('.', '/') + ".class";
      jos.putNextEntry(new JarEntry(classFile));
      ByteStreams.copy(getClass().getClassLoader().getResourceAsStream(classFile), jos);
    }

    // no plugins
    {
      List<String> args =
          ImmutableList.of(
              source.toAbsolutePath().toString(), "-processorpath", File.pathSeparator);
      StringWriter out = new StringWriter();
      Result result =
          ErrorProneCompiler.compile(args.toArray(new String[0]), new PrintWriter(out, true));
      assertThat(result).isEqualTo(Result.OK);
    }
    // with plugins
    {
      List<String> args =
          ImmutableList.of(
              source.toAbsolutePath().toString(),
              "-processorpath",
              jar.toAbsolutePath().toString());
      StringWriter out = new StringWriter();
      Result result =
          ErrorProneCompiler.compile(args.toArray(new String[0]), new PrintWriter(out, true));
      assertThat(out.toString()).contains("Using 'return' is considered harmful");
      assertThat(result).isEqualTo(Result.ERROR);
    }
  }

  @Test
  public void pluginWithFlag() throws Exception {

    Path base = tmpFolder.newFolder().toPath();
    Path source = base.resolve("test/Test.java");
    Files.createDirectories(source.getParent());
    Files.write(
        source,
        Arrays.asList(
            "package test;", //
            "public class Test {",
            "  int f() { return 42; }",
            "}"),
        UTF_8);

    Path jar = base.resolve("libproc.jar");
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
      jos.putNextEntry(new JarEntry("META-INF/services/" + BugChecker.class.getName()));
      jos.write((CPSChecker.class.getName() + "\n").getBytes(UTF_8));
      String classFile = CPSChecker.class.getName().replace('.', '/') + ".class";
      jos.putNextEntry(new JarEntry(classFile));
      ByteStreams.copy(getClass().getClassLoader().getResourceAsStream(classFile), jos);
    }

    // Plugin jar is on classpath, disabled.
    {
      List<String> args =
          ImmutableList.of(
              source.toAbsolutePath().toString(),
              "-processorpath",
              jar.toAbsolutePath().toString(),
              "-XepDisableAllChecks");
      StringWriter out = new StringWriter();
      Result result =
          ErrorProneCompiler.compile(args.toArray(new String[0]), new PrintWriter(out, true));
      assertThat(result).isEqualTo(Result.OK);
    }
    // Plugin is disabled by -XepDisableAllChecks and re-enabled with -Xep:CPSChecker:ERROR
    {
      List<String> args =
          ImmutableList.of(
              source.toAbsolutePath().toString(),
              "-processorpath",
              jar.toAbsolutePath().toString(),
              "-XepDisableAllChecks",
              "-Xep:CPSChecker:ERROR");
      StringWriter out = new StringWriter();
      Result result =
          ErrorProneCompiler.compile(args.toArray(new String[0]), new PrintWriter(out, true));
      assertThat(out.toString()).contains("Using 'return' is considered harmful");
      assertThat(result).isEqualTo(Result.ERROR);
    }
  }

  @Test
  public void paramsFiles() throws IOException {
    Path dir = tmpFolder.newFolder("tmp").toPath();
    Path source = dir.resolve("Test.java");
    Files.write(
        source,
        Joiner.on('\n')
            .join(
                ImmutableList.of(
                    "class Test {", //
                    "  boolean f(Integer i, String s) {",
                    "    return i.equals(s);",
                    "  }",
                    "}"))
            .getBytes(UTF_8));
    Path params = dir.resolve("params.txt");
    Files.write(
        params,
        Joiner.on(' ')
            .join(
                ImmutableList.of(
                    "-Xep:EqualsIncompatibleType:ERROR",
                    source.toAbsolutePath().toAbsolutePath().toString()))
            .getBytes(UTF_8));
    StringWriter output = new StringWriter();
    Result result =
        ErrorProneCompiler.builder()
            .redirectOutputTo(new PrintWriter(output, true))
            .build()
            .run(new String[] {"@" + params.toAbsolutePath().toString()});
    assertThat(result).isEqualTo(Result.ERROR);
    assertThat(output.toString()).contains("[EqualsIncompatibleType]");
  }

  /**
   * Trivial bug checker for testing command line flags. Forbids methods from returning the string
   * provided by "-XepOpt:Forbidden=<VALUE>" flag.
   */
  @BugPattern(
    name = "ForbiddenString",
    summary = "Please don't return this const value",
    category = ONE_OFF,
    severity = ERROR
  )
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
  public void checkerWithFlags() throws Exception {
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
  public void flagsAreResetOnNextCompilation() throws Exception {
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
