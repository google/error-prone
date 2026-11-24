/*
 * Copyright 2026 The Error Prone Authors.
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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Options;
import java.io.StringWriter;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TestCompilations}. */
@RunWith(JUnit4.class)
public class TestCompilationsTest {

  @Test
  public void defaultTestArgs_noProcessorArgs_prependsProcNone() {
    assertThat(TestCompilations.defaultTestArgs(ImmutableList.of()).get(0)).isEqualTo("-proc:none");
    ImmutableList<String> withExtra =
        TestCompilations.defaultTestArgs(ImmutableList.of("-Xlint:all"));
    assertThat(withExtra.get(0)).isEqualTo("-proc:none");
    assertThat(Iterables.getLast(withExtra)).isEqualTo("-Xlint:all");

    ImmutableList<String> lookalikes =
        ImmutableList.of(
            "-Xlint:-processing",
            "-proc",
            "-processor=Foo",
            "-processorpath=/foo",
            "--processor-path-extra",
            "--processor-module-path-extra",
            "-Akey=value");
    ImmutableList<String> result = TestCompilations.defaultTestArgs(lookalikes);
    assertThat(result.get(0)).isEqualTo("-proc:none");
    assertThat(result).containsAtLeastElementsIn(lookalikes).inOrder();
  }

  @Test
  public void defaultTestArgs_disableImplicitProcessingFalse_omitsProcNone() {
    assertThat(
            TestCompilations.defaultTestArgs(
                ImmutableList.of("-Xlint:all"), /* disableImplicitProcessing= */ false))
        .doesNotContain("-proc:none");
  }

  @Test
  public void defaultTestArgs_explicitProcessorFlags_omitsProcNone() {
    ImmutableList<ImmutableList<String>> explicitCases =
        ImmutableList.of(
            ImmutableList.of("-Xlint:all", "-processor", "Foo"),
            ImmutableList.of("-Xlint:all", "-processorpath", "/foo"),
            ImmutableList.of("-Xlint:all", "--processor-path", "/foo"),
            ImmutableList.of("-Xlint:all", "--processor-path=/foo"),
            ImmutableList.of("-Xlint:all", "--processor-module-path", "/foo"),
            ImmutableList.of("-Xlint:all", "--processor-module-path=/foo"),
            ImmutableList.of("-Xlint:all", "-proc:full"),
            ImmutableList.of("-Xlint:all", "-proc:only"),
            ImmutableList.of("-Xlint:all", "-Xprint"));
    for (ImmutableList<String> extraArgs : explicitCases) {
      ImmutableList<String> result = TestCompilations.defaultTestArgs(extraArgs);
      assertThat(result).doesNotContain("-proc:none");
      assertThat(result).containsAtLeastElementsIn(extraArgs).inOrder();
    }

    ImmutableList<String> explicitNone =
        TestCompilations.defaultTestArgs(ImmutableList.of("-Xlint:all", "-proc:none"));
    assertThat(explicitNone.stream().filter("-proc:none"::equals)).hasSize(1);
  }

  @Test
  public void withTestOnlyAndDeps_addsTestOnlyFlagWhenEnabled() {
    assertThat(
            TestCompilations.withTestOnlyAndDeps(
                ImmutableList.of("-Xlint:all"), /* testOnly= */ false))
        .doesNotContain("-XepCompilingTestOnlyCode");
    assertThat(
            TestCompilations.withTestOnlyAndDeps(
                ImmutableList.of("-Xlint:all"), /* testOnly= */ true))
        .contains("-XepCompilingTestOnlyCode");
    ImmutableList<String> alreadySet =
        TestCompilations.withTestOnlyAndDeps(
            ImmutableList.of("-XepCompilingTestOnlyCode"), /* testOnly= */ true);
    assertThat(alreadySet.stream().filter("-XepCompilingTestOnlyCode"::equals)).hasSize(1);
  }

  @Test
  public void getTask_appliesRequiredProductionAndTestJavacOptions() {
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavacTaskImpl task =
        TestCompilations.getTask(
            new StringWriter(),
            diagnostics,
            ImmutableList.of(),
            ImmutableList.of(FileObjects.forSourceLines("Test.java", "class Test {}")));
    Options options = Options.instance(task.getContext());
    assertThat(options.isSet(Option.PROC, "none")).isTrue();
    assertThat(options.get("compilePolicy")).isEqualTo("simple");
    assertThat(options.get("should-stop.ifError")).isEqualTo("FLOW");
    assertThat(options.get("addTypeAnnotationsToSymbol")).isEqualTo("true");
    assertThat(options.isSet(Option.PARAMETERS)).isTrue();
    assertThat(options.get(Option.XMAXERRS)).isEqualTo("500");
    assertThat(options.get(Option.XMAXWARNS)).isEqualTo("500");
  }

  @Test
  public void getTask_allowsValidRequiredOptionOverrides() {
    JavacTaskImpl task =
        TestCompilations.getTask(
            new StringWriter(),
            new DiagnosticCollector<>(),
            ImmutableList.of("-XDcompilePolicy=byfile", "--should-stop=ifError=GENERATE"),
            ImmutableList.of(FileObjects.forSourceLines("Test.java", "class Test {}")));
    Options options = Options.instance(task.getContext());
    assertThat(options.get("compilePolicy")).isEqualTo("byfile");
    assertThat(options.get("should-stop.ifError")).isEqualTo("GENERATE");
  }

  @Test
  public void getTask_rejectsUnsupportedRequiredOptions() {
    ImmutableList<JavaFileObject> units =
        ImmutableList.of(FileObjects.forSourceLines("Test.java", "class Test {}"));
    InvalidCommandLineOptionException compilePolicy =
        assertThrows(
            InvalidCommandLineOptionException.class,
            () ->
                TestCompilations.getTask(
                    new StringWriter(),
                    new DiagnosticCollector<>(),
                    ImmutableList.of("-XDcompilePolicy=byfile", "-XDcompilePolicy=bytodo"),
                    units));
    assertThat(compilePolicy).hasMessageThat().contains("-XDcompilePolicy=bytodo is not supported");

    InvalidCommandLineOptionException shouldStop =
        assertThrows(
            InvalidCommandLineOptionException.class,
            () ->
                TestCompilations.getTask(
                    new StringWriter(),
                    new DiagnosticCollector<>(),
                    ImmutableList.of("--should-stop", "ifError=INIT"),
                    units));
    assertThat(shouldStop).hasMessageThat().contains("--should-stop=ifError=INIT is not supported");

    InvalidCommandLineOptionException typeAnnotations =
        assertThrows(
            InvalidCommandLineOptionException.class,
            () ->
                TestCompilations.getTask(
                    new StringWriter(),
                    new DiagnosticCollector<>(),
                    ImmutableList.of("-XDaddTypeAnnotationsToSymbol=false"),
                    units));
    assertThat(typeAnnotations)
        .hasMessageThat()
        .contains("-XDaddTypeAnnotationsToSymbol must be set to true");
  }
}
