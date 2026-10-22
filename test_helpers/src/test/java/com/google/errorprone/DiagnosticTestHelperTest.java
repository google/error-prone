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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.DiagnosticTestHelper.LookForCheckNameInDiagnostic;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DiagnosticTestHelper}. */
@RunWith(JUnit4.class)
public class DiagnosticTestHelperTest {

  private static final JavaFileObject SOURCE =
      FileObjects.forSourceLines("Test.java", "class Test {}");

  private static final JavaFileObject OTHER =
      FileObjects.forSourceLines("Other.java", "class Other {}");

  @Test
  public void diagnosticPastTheLastLineWithoutTheCheckNameIsIgnored() {
    assertThat(
            findMismatchedLines(
                new FakeDiagnostic(SOURCE, 2, "message"), LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  @Test
  public void diagnosticPastTheLastLineWithoutTheCheckNameFailsWhenMatchingAllDiagnostics() {
    assertThat(
            findMismatchedLines(
                new FakeDiagnostic(SOURCE, 2, "message"), LookForCheckNameInDiagnostic.NO))
        .containsExactly("Saw unexpected error on line 2 of a 1-line file.");
  }

  @Test
  public void diagnosticPastTheLastLineOfAnotherFileIsLeftToThatFile() {
    assertThat(
            findMismatchedLines(
                new FakeDiagnostic(OTHER, 2, "[Check] message"), LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  @Test
  public void diagnosticOutsideTheSourcesWithoutTheCheckNameFailsWithoutACheckUnderTest() {
    DiagnosticTestHelper helper = new DiagnosticTestHelper();
    DiagnosticListener<JavaFileObject> listener = helper.collector;
    listener.report(new FakeDiagnostic(OTHER, 1, "message"));
    assertThat(
            helper.findDiagnosticsOutsideSources(
                ImmutableList.of(SOURCE), LookForCheckNameInDiagnostic.YES))
        .containsExactly(
            "Saw unexpected error on line 1 of /Other.java, which is not among the compiled"
                + " sources.");
  }

  @Test
  public void diagnosticWithoutASourceIsIgnoredPastTheLastLine() {
    assertThat(
            findMismatchedLines(
                new FakeDiagnostic(null, 2, "[Check] message"), LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  @Test
  public void diagnosticWithoutASourceIsIgnoredOutsideTheSources() {
    assertThat(
            findDiagnosticsOutsideSources(
                new FakeDiagnostic(null, 1, "[Check] message"), LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  @Test
  public void diagnosticInAFileOutsideTheSourcesFails() {
    assertThat(
            findDiagnosticsOutsideSources(
                new FakeDiagnostic(OTHER, 1, "[Check] message"), LookForCheckNameInDiagnostic.YES))
        .containsExactly(
            "Saw unexpected error on line 1 of /Other.java, which is not among the compiled"
                + " sources.");
  }

  @Test
  public void diagnosticInASourceIsLeftToTheLineByLineCheck() {
    assertThat(
            findDiagnosticsOutsideSources(
                new FakeDiagnostic(SOURCE, 1, "[Check] message"), LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  @Test
  public void diagnosticOutsideTheSourcesWithoutTheCheckNameIsIgnored() {
    assertThat(
            findDiagnosticsOutsideSources(
                new FakeDiagnostic(OTHER, 1, "message"), LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  @Test
  public void diagnosticOutsideTheSourcesWithoutTheCheckNameFailsWhenMatchingAllDiagnostics() {
    assertThat(
            findDiagnosticsOutsideSources(
                new FakeDiagnostic(OTHER, 1, "message"), LookForCheckNameInDiagnostic.NO))
        .containsExactly(
            "Saw unexpected error on line 1 of /Other.java, which is not among the compiled"
                + " sources.");
  }

  /**
   * javac reports some notes, such as the one that a file uses unchecked operations, against a file
   * and no line in it.
   */
  @Test
  public void diagnosticOutsideTheSourcesWithoutALineIsIgnored() {
    assertThat(
            findDiagnosticsOutsideSources(
                new FakeDiagnostic(OTHER, Diagnostic.NOPOS, "[Check] message"),
                LookForCheckNameInDiagnostic.YES))
        .isEmpty();
  }

  private static ImmutableList<String> findMismatchedLines(
      Diagnostic<JavaFileObject> diagnostic,
      LookForCheckNameInDiagnostic lookForCheckNameInDiagnostic) {
    try {
      return helperReporting(diagnostic).findMismatchedLines(SOURCE, lookForCheckNameInDiagnostic);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static ImmutableList<String> findDiagnosticsOutsideSources(
      Diagnostic<JavaFileObject> diagnostic,
      LookForCheckNameInDiagnostic lookForCheckNameInDiagnostic) {
    return helperReporting(diagnostic)
        .findDiagnosticsOutsideSources(ImmutableList.of(SOURCE), lookForCheckNameInDiagnostic);
  }

  /** Returns a helper for the check named {@code Check} that has collected {@code diagnostic}. */
  private static DiagnosticTestHelper helperReporting(Diagnostic<JavaFileObject> diagnostic) {
    DiagnosticTestHelper helper = new DiagnosticTestHelper("Check");
    DiagnosticListener<JavaFileObject> listener = helper.collector;
    listener.report(diagnostic);
    return helper;
  }

  private record FakeDiagnostic(@Nullable JavaFileObject source, long lineNumber, String message)
      implements Diagnostic<JavaFileObject> {
    @Override
    public Kind getKind() {
      return Kind.ERROR;
    }

    @Override
    public @Nullable JavaFileObject getSource() {
      return source;
    }

    @Override
    public long getPosition() {
      return NOPOS;
    }

    @Override
    public long getStartPosition() {
      return NOPOS;
    }

    @Override
    public long getEndPosition() {
      return NOPOS;
    }

    @Override
    public long getLineNumber() {
      return lineNumber;
    }

    @Override
    public long getColumnNumber() {
      return NOPOS;
    }

    @Override
    public String getCode() {
      return "compiler.err.error.prone";
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }
  }
}
