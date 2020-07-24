/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.stringLiteral;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.parser.Tokens.Comment;
import java.util.Optional;

/**
 * Finds occurrences of {@code @SuppressWarnings} where there is definitely no explanation for why
 * it is safe.
 *
 * <p>The Google style guide mandates this for <em>all</em> suppressions; this is only matching on
 * {@code deprecation} as a trial.
 */
@BugPattern(
    name = "SuppressWarningsWithoutExplanation",
    summary =
        "Use of @SuppressWarnings should be accompanied by a comment describing why the warning is"
            + " safe to ignore.",
    severity = WARNING,
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s8.4.2-how-to-handle-a-warning"
    )
public final class SuppressWarningsWithoutExplanation extends BugChecker
    implements CompilationUnitTreeMatcher {
  private static final Matcher<AnnotationTree> SUPPRESS_WARNINGS =
      allOf(
          isSameType(SuppressWarnings.class),
          hasArgumentWithValue("value", stringLiteral("deprecation")));

  private final boolean emitDummyFixes;

  public SuppressWarningsWithoutExplanation() {
    this(false);
  }

  public SuppressWarningsWithoutExplanation(boolean emitDummyFixes) {
    this.emitDummyFixes = emitDummyFixes;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (!ASTHelpers.getGeneratedBy(state).isEmpty()) {
      return NO_MATCH;
    }
    ImmutableRangeSet<Long> linesWithComments = linesWithComments(state);
    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitAnnotation(AnnotationTree annotationTree, Void unused) {
        if (!SUPPRESS_WARNINGS.matches(annotationTree, state)) {
          return super.visitAnnotation(annotationTree, null);
        }
        LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();

        Tree parent = getCurrentPath().getParentPath().getLeaf();
        // Expand by +/- one to accept comments either before or after the suppression.
        Range<Long> linesCovered =
            Range.closed(
                lineMap.getLineNumber(getStartPosition(parent)) - 1,
                lineMap.getLineNumber(state.getEndPosition(parent)) + 1);
        if (!linesWithComments.intersects(linesCovered)) {
          state.reportMatch(
              describeMatch(
                  annotationTree,
                  emitDummyFixes
                      ? Optional.of(SuggestedFix.postfixWith(annotationTree, " // Safe because..."))
                      : Optional.empty()));
        }
        return super.visitAnnotation(annotationTree, null);
      }
    }.scan(tree, null);
    return NO_MATCH;
  }

  private static ImmutableRangeSet<Long> linesWithComments(VisitorState state) {
    RangeSet<Long> lines = TreeRangeSet.create();
    ErrorProneTokens tokens = new ErrorProneTokens(state.getSourceCode().toString(), state.context);
    LineMap lineMap = tokens.getLineMap();
    for (ErrorProneToken token : tokens.getTokens()) {
      for (Comment comment : token.comments()) {
        lines.add(
            Range.closed(
                lineMap.getLineNumber(comment.getSourcePos(0)),
                lineMap.getLineNumber(comment.getSourcePos(comment.getText().length() - 1))));
      }
    }
    return ImmutableRangeSet.copyOf(lines);
  }
}
