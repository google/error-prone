/*
 * Copyright 2014 The Error Prone Authors.
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
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.CharStreams;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BugPatternFileGeneratorTest {

  @Rule public TemporaryFolder tmpfolder = new TemporaryFolder();

  private Path wikiDir;
  private Path explanationDirBase;

  @Before
  public void setUp() throws Exception {
    wikiDir = tmpfolder.newFolder("wiki").toPath();
    explanationDirBase = tmpfolder.newFolder("explanations").toPath();
  }

  private static BugPatternInstance deadExceptionTestInfo() {
    BugPatternInstance instance = new BugPatternInstance();
    instance.className = "com.google.errorprone.bugpatterns.DeadException";
    instance.name = "DeadException";
    instance.summary = "Exception created but not thrown";
    instance.explanation =
        "The exception is created with new, but is not thrown, and the reference is lost.";
    instance.altNames = new String[] {"ThrowableInstanceNeverThrown"};
    instance.tags = new String[] {"LikelyError"};
    instance.severity = SeverityLevel.ERROR;
    instance.suppressionAnnotations = new String[] {"java.lang.SuppressWarnings.class"};
    return instance;
  }

  private static final String BUGPATTERN_LINE;

  static {
    BugPatternInstance instance = deadExceptionTestInfo();
    BUGPATTERN_LINE = new Gson().toJson(instance);
  }

  private static final String BUGPATTERN_LINE_SIDECAR;

  static {
    BugPatternInstance instance = deadExceptionTestInfo();
    instance.explanation = "";
    BUGPATTERN_LINE_SIDECAR = new Gson().toJson(instance);
  }

  // Assert that the generator produces the same output it did before.
  // This is brittle, but you can open the golden file
  // src/test/resources/com/google/errorprone/DeadException.md
  // in the same Jekyll environment you use for prod, and verify it looks good.
  @Test
  public void regressionTest_frontmatter_pygments() throws Exception {
    BugPatternFileGenerator generator =
        new BugPatternFileGenerator(
            wikiDir, explanationDirBase, true, null, input -> input.severity);
    generator.processLine(BUGPATTERN_LINE);
    String expected =
        CharStreams.toString(
            new InputStreamReader(
                getClass().getResourceAsStream("testdata/DeadException_frontmatter_pygments.md"),
                UTF_8));
    String actual =
        CharStreams.toString(Files.newBufferedReader(wikiDir.resolve("DeadException.md"), UTF_8));
    assertThat(actual.trim()).isEqualTo(expected.trim());
  }

  @Test
  public void regressionTest_nofrontmatter_gfm() throws Exception {
    BugPatternFileGenerator generator =
        new BugPatternFileGenerator(
            wikiDir,
            explanationDirBase,
            false,
            null,
            input -> input.severity);
    generator.processLine(BUGPATTERN_LINE);
    String expected =
        CharStreams.toString(
            new InputStreamReader(
                getClass().getResourceAsStream("testdata/DeadException_nofrontmatter_gfm.md"),
                UTF_8));
    String actual = new String(Files.readAllBytes(wikiDir.resolve("DeadException.md")), UTF_8);
    assertThat(actual.trim()).isEqualTo(expected.trim());
  }

  @Test
  public void regressionTest_sidecar() throws Exception {
    BugPatternFileGenerator generator =
        new BugPatternFileGenerator(
            wikiDir,
            explanationDirBase,
            false,
            null,
            input -> input.severity);
    Files.write(
        explanationDirBase.resolve("DeadException.md"),
        Arrays.asList(
            "The exception is created with new, but is not thrown, and the reference is lost."),
        UTF_8);
    generator.processLine(BUGPATTERN_LINE_SIDECAR);
    String expected =
        CharStreams.toString(
            new InputStreamReader(
                getClass().getResourceAsStream("testdata/DeadException_nofrontmatter_gfm.md"),
                UTF_8));
    String actual = new String(Files.readAllBytes(wikiDir.resolve("DeadException.md")), UTF_8);
    assertThat(actual.trim()).isEqualTo(expected.trim());
  }

  @Test
  public void testEscapeAngleBracketsInSummary() throws Exception {
    // Create a BugPattern with angle brackets in the summary
    BugPatternInstance instance = new BugPatternInstance();
    instance.className = "com.google.errorprone.bugpatterns.DontDoThis";
    instance.name = "DontDoThis";
    instance.summary = "Don't do this; do List<Foo> instead";
    instance.explanation = "This is a bad idea, you want `List<Foo>` instead";
    instance.altNames = new String[0];
    instance.tags = new String[] {"LikelyError"};
    instance.severity = SeverityLevel.ERROR;
    instance.suppressionAnnotations = new String[] {"java.lang.SuppressWarnings.class"};

    // Write markdown file
    BugPatternFileGenerator generator =
        new BugPatternFileGenerator(
            wikiDir,
            explanationDirBase,
            false,
            null,
            input -> input.severity);
    generator.processLine(new Gson().toJson(instance));
    String expected =
        CharStreams.toString(
            new InputStreamReader(
                getClass().getResourceAsStream("testdata/DontDoThis_nofrontmatter_gfm.md"), UTF_8));
    String actual = new String(Files.readAllBytes(wikiDir.resolve("DontDoThis.md")), UTF_8);
    assertThat(actual.trim()).isEqualTo(expected.trim());
  }
}
