/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;
import com.google.common.truth.Correspondence;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Utility class for tests which need to assert on the diagnostics produced during compilation.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class DiagnosticTestHelper {
  // When testing a single error-prone check, the name of the check. Used to validate diagnostics.
  // Null if not testing a single error-prone check.
  private final String checkName;

  private final Map<String, Predicate<? super String>> expectedErrorMsgs = new HashMap<>();

  /** Construct a {@link DiagnosticTestHelper} not associated with a specific check. */
  public DiagnosticTestHelper() {
    this(null);
  }

  /** Construct a {@link DiagnosticTestHelper} for a check with the given name. */
  public DiagnosticTestHelper(String checkName) {
    this.checkName = checkName;
  }

  public final ClearableDiagnosticCollector<JavaFileObject> collector =
      new ClearableDiagnosticCollector<>();

  public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
    return collector.getDiagnostics();
  }

  public void clearDiagnostics() {
    collector.clear();
  }

  public String describe() {
    StringBuilder stringBuilder = new StringBuilder().append("Diagnostics:\n");
    for (Diagnostic<? extends JavaFileObject> diagnostic : getDiagnostics()) {
      stringBuilder
          .append("  [")
          .append(diagnostic.getLineNumber())
          .append(":")
          .append(diagnostic.getColumnNumber())
          .append("]\t");
      stringBuilder.append(diagnostic.getMessage(Locale.getDefault()).replace("\n", "\\n"));
      stringBuilder.append("\n");
    }
    return stringBuilder.toString();
  }

  public static Matcher<Diagnostic<? extends JavaFileObject>> diagnosticOnLine(
      URI fileUri, long line) {
    return new TypeSafeDiagnosingMatcher<Diagnostic<? extends JavaFileObject>>() {
      @Override
      public boolean matchesSafely(
          Diagnostic<? extends JavaFileObject> item, Description mismatchDescription) {
        if (item.getSource() == null) {
          mismatchDescription
              .appendText("diagnostic not attached to a file: ")
              .appendValue(item.getMessage(ENGLISH));
          return false;
        }

        if (!item.getSource().toUri().equals(fileUri)) {
          mismatchDescription.appendText("diagnostic not in file ").appendValue(fileUri);
          return false;
        }

        if (item.getLineNumber() != line) {
          mismatchDescription
              .appendText("diagnostic not on line ")
              .appendValue(item.getLineNumber());
          return false;
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a diagnostic on line ").appendValue(line);
      }
    };
  }

  public static Matcher<Diagnostic<? extends JavaFileObject>> diagnosticOnLine(
      URI fileUri, long line, Predicate<? super String> matcher) {
    return new TypeSafeDiagnosingMatcher<Diagnostic<? extends JavaFileObject>>() {
      @Override
      public boolean matchesSafely(
          Diagnostic<? extends JavaFileObject> item, Description mismatchDescription) {
        if (item.getSource() == null) {
          mismatchDescription
              .appendText("diagnostic not attached to a file: ")
              .appendValue(item.getMessage(ENGLISH));
          return false;
        }

        if (!item.getSource().toUri().equals(fileUri)) {
          mismatchDescription.appendText("diagnostic not in file ").appendValue(fileUri);
          return false;
        }

        if (item.getLineNumber() != line) {
          mismatchDescription
              .appendText("diagnostic not on line ")
              .appendValue(item.getLineNumber());
          return false;
        }

        if (!matcher.test(item.getMessage(Locale.getDefault()))) {
          mismatchDescription.appendText("diagnostic does not match ").appendValue(matcher);
          return false;
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description
            .appendText("a diagnostic on line ")
            .appendValue(line)
            .appendText(" that matches \n")
            .appendValue(matcher)
            .appendText("\n");
      }
    };
  }

  public static final Correspondence<Diagnostic<? extends JavaFileObject>, String>
      DIAGNOSTIC_CONTAINING =
          Correspondence.from(
              (diagnostic, message) -> diagnostic.getMessage(Locale.getDefault()).contains(message),
              "diagnostic containing");

  /**
   * Comment that marks a bug on the next line in a test file. For example, "// BUG: Diagnostic
   * contains: foo.bar()", where "foo.bar()" is a string that should be in the diagnostic for the
   * line. Multiple expected strings may be separated by newlines, e.g. // BUG: Diagnostic contains:
   * foo.bar() // bar.baz() // baz.foo()
   */
  private static final String BUG_MARKER_COMMENT_INLINE = "// BUG: Diagnostic contains:";

  private static final String BUG_MARKER_COMMENT_LOOKUP = "// BUG: Diagnostic matches:";
  private final Set<String> usedLookupKeys = new HashSet<>();

  enum LookForCheckNameInDiagnostic {
    YES,
    NO;
  }

  /**
   * Expects an error message matching {@code matcher} at the line below a comment matching the key.
   * For example, given the source
   *
   * <pre>
   *   // BUG: Diagnostic matches: X
   *   a = b + c;
   * </pre>
   *
   * ... you can use {@code expectErrorMessage("X", Predicates.containsPattern("Can't add b to
   * c"));}
   *
   * <p>Error message keys that don't match any diagnostics will cause test to fail.
   */
  public void expectErrorMessage(String key, Predicate<? super String> matcher) {
    expectedErrorMsgs.put(key, matcher);
  }

  /**
   * Asserts that the diagnostics contain a diagnostic on each line of the source file that matches
   * our bug marker pattern. Parses the bug marker pattern for the specific string to look for in
   * the diagnostic.
   *
   * @param source File in which to find matching lines
   */
  public void assertHasDiagnosticOnAllMatchingLines(
      JavaFileObject source, LookForCheckNameInDiagnostic lookForCheckNameInDiagnostic)
      throws IOException {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = getDiagnostics();
    LineNumberReader reader =
        new LineNumberReader(CharSource.wrap(source.getCharContent(false)).openStream());
    do {
      String line = reader.readLine();
      if (line == null) {
        break;
      }

      List<Predicate<? super String>> predicates = null;
      if (line.contains(BUG_MARKER_COMMENT_INLINE)) {
        // Diagnostic must contain all patterns from the bug marker comment.
        List<String> patterns = extractPatterns(line, reader, BUG_MARKER_COMMENT_INLINE);
        predicates = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
          predicates.add(new SimpleStringContains(pattern));
        }
      } else if (line.contains(BUG_MARKER_COMMENT_LOOKUP)) {
        int markerLineNumber = reader.getLineNumber();
        List<String> lookupKeys = extractPatterns(line, reader, BUG_MARKER_COMMENT_LOOKUP);
        predicates = new ArrayList<>(lookupKeys.size());
        for (String lookupKey : lookupKeys) {
          assertWithMessage(
                  "No expected error message with key [%s] as expected from line [%s] "
                      + "with diagnostic [%s]",
                  lookupKey, markerLineNumber, line.trim())
              .that(expectedErrorMsgs.containsKey(lookupKey))
              .isTrue();
          predicates.add(expectedErrorMsgs.get(lookupKey));
          usedLookupKeys.add(lookupKey);
        }
      }

      if (predicates != null) {
        int lineNumber = reader.getLineNumber();
        for (Predicate<? super String> predicate : predicates) {
          Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> patternMatcher =
              hasItem(diagnosticOnLine(source.toUri(), lineNumber, predicate));
          assertWithMessage(
                  "Did not see an error on line %s matching %s. %s",
                  lineNumber, predicate, allErrors(diagnostics))
              .that(patternMatcher.matches(diagnostics))
              .isTrue();
        }

        if (checkName != null && lookForCheckNameInDiagnostic == LookForCheckNameInDiagnostic.YES) {
          // Diagnostic must contain check name.
          Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> checkNameMatcher =
              hasItem(
                  diagnosticOnLine(
                      source.toUri(), lineNumber, new SimpleStringContains("[" + checkName + "]")));
          assertWithMessage(
                  "Did not see an error on line %s containing [%s]. %s",
                  lineNumber, checkName, allErrors(diagnostics))
              .that(checkNameMatcher.matches(diagnostics))
              .isTrue();
        }

      } else {
        int lineNumber = reader.getLineNumber();
        Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
            hasItem(diagnosticOnLine(source.toUri(), lineNumber));
        if (matcher.matches(diagnostics)) {
          fail("Saw unexpected error on line " + lineNumber + ". " + allErrors(diagnostics));
        }
      }
    } while (true);
    reader.close();
  }

  private static String allErrors(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    if (diagnostics.isEmpty()) {
      return "There were no errors.";
    }
    return "All errors:\n"
        + diagnostics.stream().map(Object::toString).collect(Collectors.joining("\n\n"));
  }

  /** Returns the lookup keys that weren't used. */
  public Set<String> getUnusedLookupKeys() {
    return Sets.difference(expectedErrorMsgs.keySet(), usedLookupKeys);
  }

  /**
   * Extracts the patterns from a bug marker comment.
   *
   * @param line The first line of the bug marker comment
   * @param reader A reader for the test file
   * @param matchString The bug marker comment match string.
   * @return A list of patterns that the diagnostic is expected to contain
   */
  private static List<String> extractPatterns(
      String line, BufferedReader reader, String matchString) throws IOException {
    int bugMarkerIndex = line.indexOf(matchString);
    if (bugMarkerIndex < 0) {
      throw new IllegalArgumentException("Line must contain bug marker prefix");
    }
    List<String> result = new ArrayList<>();
    String restOfLine = line.substring(bugMarkerIndex + matchString.length()).trim();
    result.add(restOfLine);
    line = reader.readLine().trim();
    while (line.startsWith("//")) {
      restOfLine = line.substring(2).trim();
      result.add(restOfLine);
      line = reader.readLine().trim();
    }

    return result;
  }

  private static class SimpleStringContains implements Predicate<String> {
    private final String pattern;

    SimpleStringContains(String pattern) {
      this.pattern = pattern;
    }

    @Override
    public boolean test(String input) {
      return input.contains(pattern);
    }

    @Override
    public String toString() {
      return pattern;
    }
  }

  private static class ClearableDiagnosticCollector<S> implements DiagnosticListener<S> {
    private final List<Diagnostic<? extends S>> diagnostics = new ArrayList<>();

    @Override
    public void report(Diagnostic<? extends S> diagnostic) {
      diagnostics.add(diagnostic);
    }

    List<Diagnostic<? extends S>> getDiagnostics() {
      return ImmutableList.copyOf(diagnostics);
    }

    void clear() {
      diagnostics.clear();
    }
  }
}
