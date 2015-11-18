/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.util.ErrorProneToken;

import com.google.errorprone.matchers.Description;

import com.sun.source.tree.CatchTree;
import com.sun.tools.javac.parser.Tokens.Comment;


/**
 * "TODO" and "FIXME" should not appear in the error handling
 * logic, because it is often the last line of defense.
 * <p>
 * For more detail, refer to the paper:
 * "Simple Testing Can Prevent Most Critical Failures: 
 *  An Analysis of Production Failures in Distributed Data-intensive Systems"
 *  Yuan et al. Proceedings of the 11th Symposium on Operating Systems Design 
 *  and Implementation (OSDI), 2014
 *
 * @author yuan@eecg.utoronto.ca (Ding Yuan)
 */
@BugPattern(name = "TodoInCatch",
    summary = "TODO or FIXME is found in the catch block, indicating "
            + "potential incorrect handling of the exception",
    explanation = "TODO or FIXME is found in the catch block, indicating "
            + "potential incorrect handling of the exception. In production systems, "
            + "Murphy's law often applies: Anything that can go wrong, will go wrong. "
            + "Therefore it is particularly important to properly handle the exceptions "
            + "regardless how rarely they are likely to occur, especially given that they "
            + "are often the last line of defense.\n\n"
            + "Read \"[Simple Testing Can Prevent Most Critical Failures] "
            + "(http://www.eecg.toronto.edu/~yuan/papers/failure_analysis_osdi14.pdf)\" "
            + "for more detailed discussions on the harm of this pattern. ",
    category = JDK, maturity = EXPERIMENTAL, severity = WARNING)
public class TodoInCatch extends BugChecker implements CatchTreeMatcher {
  @Override
  public Description matchCatch (CatchTree catchTree, VisitorState state) {
    String catchSource = state.getSourceForNode(catchTree);
    if (catchSource.contains("TODO") || catchSource.contains("FIXME")) {
      // Getting the tokens for a node is expensive, so we only do it if we think there is a match here
      for (ErrorProneToken token : state.getTokensForNode(catchTree)) {
        for (Comment comment : token.comments()) {
          if (comment.getText().contains("TODO") || comment.getText().contains("FIXME")) {
            return describeMatch(catchTree);
          }
        }
      }
    }
    return Description.NO_MATCH;
  }
}
