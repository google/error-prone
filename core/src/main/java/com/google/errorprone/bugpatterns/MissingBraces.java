/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EnhancedForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WhileLoopTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "MissingBraces",
    summary =
        "The Google Java Style Guide requires braces to be used with if, else, for, do and while"
            + " statements, even when the body is empty or contains only a single statement.",
    severity = SeverityLevel.SUGGESTION,
    tags = StandardTags.STYLE,
    linkType = LinkType.CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s4.1.1-braces-always-used")
public class MissingBraces extends BugChecker
    implements IfTreeMatcher,
        ForLoopTreeMatcher,
        DoWhileLoopTreeMatcher,
        WhileLoopTreeMatcher,
        EnhancedForLoopTreeMatcher {

  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    check(tree.getThenStatement(), state);
    if (tree.getElseStatement() != null
        && !tree.getElseStatement().getKind().equals(Tree.Kind.IF)) {
      check(tree.getElseStatement(), state);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
    check(tree.getStatement(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchForLoop(ForLoopTree tree, VisitorState state) {
    check(tree.getStatement(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
    check(tree.getStatement(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchWhileLoop(WhileLoopTree tree, VisitorState state) {
    check(tree.getStatement(), state);
    return NO_MATCH;
  }

  void check(StatementTree tree, VisitorState state) {
    if (tree != null && !(tree instanceof BlockTree)) {
      state.reportMatch(
          describeMatch(
              tree, SuggestedFix.builder().prefixWith(tree, "{").postfixWith(tree, "}").build()));
    }
  }
}
