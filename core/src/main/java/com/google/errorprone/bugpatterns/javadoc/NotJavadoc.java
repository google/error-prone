/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getJavadoccableTrees;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isRecord;
import static com.google.errorprone.util.ErrorProneTokens.getTokens;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.ErrorProneComment.ErrorProneCommentStyle;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

/** A BugPattern; see the summary. */
@BugPattern(
    summary = "Avoid using `/**` for comments which aren't actually Javadoc.",
    severity = WARNING,
    documentSuppression = false)
public final class NotJavadoc extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableMap<Integer, TreePath> javadocableTrees = getJavadoccableTrees(tree);
    ImmutableMap<Integer, JavadocableTreeKind> seeminglyJavadocableTrees =
        getSeeminglyJavadocableTrees(tree);
    ImmutableRangeSet<Integer> suppressedRegions = suppressedRegions(state);
    for (ErrorProneToken token : getTokens(state.getSourceCode().toString(), state.context)) {
      for (ErrorProneComment comment : token.comments()) {
        switch (comment.getStyle()) {
          case JAVADOC_BLOCK -> {
            if (comment.getText().equals("/**/")) {
              continue;
            }
          }
          case JAVADOC_LINE -> {}
          default -> {
            continue;
          }
        }
        // if this is a valid location for Javadoc, skip over this comment
        if (javadocableTrees.containsKey(token.pos())) {
          continue;
        }

        if (suppressedRegions.intersects(
            Range.closed(
                comment.getSourcePos(0), comment.getSourcePos(comment.getText().length() - 1)))) {
          continue;
        }

        String message =
            switch (seeminglyJavadocableTrees.get(token.pos())) {
              case CLASS ->
                  "This comment is attached to a local class, but local classes cannot be"
                      + " documented with Javadoc. Please convert this to a regular comment.";
              case METHOD ->
                  "This comment is attached to a method inside a local class, but methods inside"
                      + " local classes cannot be documented with Javadoc. Please convert this to"
                      + " a regular comment.";
              case RECORD_COMPONENT ->
                  "This comment seems to be attached to a record component,"
                      + " but record components cannot be documented with Javadoc. Please move this"
                      + " to an @param tag on the record class.";
              case null -> message();
            };
        var description =
            buildDescription(getDiagnosticPosition(comment.getSourcePos(0), tree))
                .setMessage(message);
        // we only emit a fix for Classic Javadoc (not Markdown Javadoc)
        if (comment.getStyle().equals(ErrorProneCommentStyle.JAVADOC_BLOCK)) {
          int endPos = 2;
          while (comment.getText().charAt(endPos) == '*') {
            endPos++;
          }
          description.addFix(
              replace(comment.getSourcePos(1), comment.getSourcePos(endPos - 1), ""));
        }
        state.reportMatch(description.build());
      }
    }
    return NO_MATCH;
  }

  private enum JavadocableTreeKind {
    CLASS,
    METHOD,
    RECORD_COMPONENT
  }

  private static ImmutableMap<Integer, JavadocableTreeKind> getSeeminglyJavadocableTrees(
      CompilationUnitTree tree) {
    ImmutableMap.Builder<Integer, JavadocableTreeKind> builder = ImmutableMap.builder();
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitClass(ClassTree tree, Void unused) {
            builder.put(getStartPosition(tree), JavadocableTreeKind.CLASS);
            return super.visitClass(tree, null);
          }

          @Override
          public Void visitMethod(MethodTree tree, Void unused) {
            builder.put(getStartPosition(tree), JavadocableTreeKind.METHOD);
            return super.visitMethod(tree, null);
          }

          @Override
          public Void visitVariable(VariableTree tree, Void unused) {
            if (isRecord(getSymbol(tree))) {
              builder.put(getStartPosition(tree), JavadocableTreeKind.RECORD_COMPONENT);
            }
            return super.visitVariable(tree, null);
          }
        },
        null);
    return builder.buildKeepingLast();
  }
}
