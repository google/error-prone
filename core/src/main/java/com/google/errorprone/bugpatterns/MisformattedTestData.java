/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.SourceVersion.supportsTextBlocks;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.ArrayList;
import java.util.List;

/** See the summary. */
@BugPattern(
    severity = WARNING,
    summary = "This test data will be more readable if correctly formatted.")
public final class MisformattedTestData extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    // We're not only matching text blocks below, just taking the fact that it's a single literal
    // argument containing source code as a sign that it should be formatted in a text block.
    if (!supportsTextBlocks(state.context)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    List<LiteralTree> sourceTrees = new ArrayList<>();
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        process(tree, state, fixBuilder, sourceTrees);
        return super.visitMethodInvocation(tree, null);
      }
    }.scan(state.getPath(), null);
    if (sourceTrees.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix combinedFix = fixBuilder.build();
    for (var sourceTree : sourceTrees) {
      state.reportMatch(buildDescription(sourceTree).addFix(combinedFix).build());
    }

    return NO_MATCH;
  }

  private static void process(
      MethodInvocationTree tree,
      VisitorState state,
      SuggestedFix.Builder fixBuilder,
      List<LiteralTree> sourceTrees) {
    if (!ADD_SOURCE_CALL.matches(tree, state)) {
      return;
    }
    if (tree.getArguments().size() != 2) {
      return;
    }
    List<LiteralTree> literalTrees = new ArrayList<>();
    new com.sun.source.util.TreeScanner<Void, Void>() {
      @Override
      public Void visitLiteral(LiteralTree node, Void unused) {
        if (node.getValue() instanceof String) {
          literalTrees.add(node);
        }
        return super.visitLiteral(node, null);
      }
    }.scan(tree.getArguments().get(1), null);

    for (var literalTree : literalTrees) {
      var sourceValue = literalTree.getValue();
      if (!(sourceValue instanceof String string)) {
        continue;
      }

      Formatter formatter = new Formatter();
      String formattedSource;
      try {
        var sourceWithProtectedBugMarkers =
            SourceWithProtectedBugMarkers.protectBugMarkerComments(string);
        formattedSource =
            sourceWithProtectedBugMarkers.restore(
                formatter.formatSourceAndFixImports(sourceWithProtectedBugMarkers.source()));
      } catch (FormatterException exception) {
        continue;
      }
      if (formattedSource.trim().equals(string.trim())) {
        continue;
      }

      int literalStart = getStartPosition(literalTree);
      CharSequence sourceCode = state.getSourceCode();
      int lineStart = literalStart;
      while (lineStart > 0 && sourceCode.charAt(lineStart - 1) != '\n') {
        lineStart--;
      }
      String linePrefix = sourceCode.subSequence(lineStart, literalStart).toString();
      String spaces =
          linePrefix.substring(0, linePrefix.length() - linePrefix.stripLeading().length());

      String replacement =
          "\"\"\"\n"
              + LINE_SPLITTER
                  .splitToStream(escape(formattedSource))
                  .map(line -> line.isEmpty() ? "" : spaces + line)
                  .collect(joining("\n"))
              + spaces
              + "\"\"\"";
      fixBuilder.replace(literalTree, replacement);
      sourceTrees.add(literalTree);
    }
  }

  // TODO(ghm): Consider generalising this via an annotation.
  private static final Matcher<ExpressionTree> ADD_SOURCE_CALL =
      anyOf(
          instanceMethod()
              .onExactClass("com.google.errorprone.CompilationTestHelper")
              .named("addSourceLines"),
          instanceMethod()
              .onExactClass("com.google.errorprone.BugCheckerRefactoringTestHelper")
              .named("addInputLines"),
          instanceMethod()
              .onExactClass("com.google.errorprone.BugCheckerRefactoringTestHelper.ExpectOutput")
              .named("addOutputLines"));

  private static final Matcher<ExpressionTree> ADD_OUTPUT_LINES =
      instanceMethod()
          .onExactClass("com.google.errorprone.BugCheckerRefactoringTestHelper.ExpectOutput")
          .named("addOutputLines");

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');

  private record SourceWithProtectedBugMarkers(
      String source, String placeholderPrefix, List<String> comments) {

    private static final String DIAGNOSTIC_CONTAINS_MARKER = "// BUG: Diagnostic contains:";

    private static final String DIAGNOSTIC_MATCHES_MARKER = "// BUG: Diagnostic matches:";

    /**
     * Replace error diagnostic comments with placeholders so they are not modified during
     * formatting
     */
    private static SourceWithProtectedBugMarkers protectBugMarkerComments(String source) {
      String placeholderPrefix = "__EP_BUG_MARKER_";
      while (source.contains(placeholderPrefix)) {
        placeholderPrefix += "_";
      }

      List<String> comments = new ArrayList<>();
      List<String> lines = LINE_SPLITTER.splitToList(source);
      StringBuilder result = new StringBuilder(source.length());
      boolean inBugMarkerComment = false;
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        String trimmedLine = line.trim();
        boolean startsBugMarkerComment =
            line.indexOf(DIAGNOSTIC_CONTAINS_MARKER) != -1
                || line.indexOf(DIAGNOSTIC_MATCHES_MARKER) != -1;
        if (!startsBugMarkerComment && !(inBugMarkerComment && trimmedLine.startsWith("//"))) {
          inBugMarkerComment = false;
        } else {
          inBugMarkerComment = true;
          int commentStart = line.indexOf("//");
          // still strip trailing whitespace from bug marker comments, as DiagnosticTestHelper also
          // ignores such whitespace
          comments.add(line.substring(commentStart).stripTrailing());
          line =
              line.substring(0, commentStart)
                  + SourceWithProtectedBugMarkers.placeholder(
                      placeholderPrefix, comments.size() - 1);
        }
        if (i > 0) {
          result.append('\n');
        }
        result.append(line);
      }
      return new SourceWithProtectedBugMarkers(result.toString(), placeholderPrefix, comments);
    }

    /** restore original diagnostic comments into the formatted test input source */
    private String restore(String formattedSource) {
      for (int i = 0; i < comments.size(); i++) {
        formattedSource =
            formattedSource.replace(placeholder(placeholderPrefix, i), comments.get(i));
      }
      return formattedSource;
    }

    private static String placeholder(String prefix, int index) {
      return "// " + prefix + index + "__";
    }
  }

  private static String escape(String line) {
    return ESCAPER.escape(line).replace("\"\"\"", "\\\"\"\"");
  }

  private static final Escaper ESCAPER = Escapers.builder().addEscape('\\', "\\\\").build();
}
