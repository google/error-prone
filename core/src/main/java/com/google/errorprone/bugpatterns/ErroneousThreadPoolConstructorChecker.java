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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import java.util.List;

/**
 * ErrorProne checker to generate warning whenever {@link java.util.concurrent.ThreadPoolExecutor}
 * is constructed with different {@code corePoolSize} and {@code maximumPoolSize} using an unbounded
 * {@code workQueue}
 */
@BugPattern(
    summary = "Thread pool size will never go beyond corePoolSize if an unbounded queue is used",
    severity = WARNING)
public final class ErroneousThreadPoolConstructorChecker extends BugChecker
    implements NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> THREAD_POOL_CONSTRUCTOR_MATCHER =
      constructor().forClass("java.util.concurrent.ThreadPoolExecutor");
  private static final Matcher<ExpressionTree> UNBOUNDED_WORK_QUEUE_CONSTRUCTOR_MATCHER =
      anyOf(
          constructor().forClass("java.util.concurrent.LinkedBlockingDeque").withNoParameters(),
          constructor()
              .forClass("java.util.concurrent.LinkedBlockingDeque")
              .withParameters("java.util.Collection"),
          constructor().forClass("java.util.concurrent.LinkedBlockingQueue").withNoParameters(),
          constructor()
              .forClass("java.util.concurrent.LinkedBlockingQueue")
              .withParameters("java.util.Collection"),
          constructor().forClass("java.util.concurrent.LinkedTransferQueue"),
          constructor().forClass("java.util.concurrent.PriorityBlockingQueue"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!THREAD_POOL_CONSTRUCTOR_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    Integer corePoolSize = ASTHelpers.constValue(arguments.get(0), Integer.class);
    Integer maximumPoolSize = ASTHelpers.constValue(arguments.get(1), Integer.class);
    if (corePoolSize == null || maximumPoolSize == null || corePoolSize.equals(maximumPoolSize)) {
      return Description.NO_MATCH;
    }
    // This is a special case, as ThreadPoolExecutor ensures starting the first thread in the pool
    if (corePoolSize == 0 && maximumPoolSize == 1) {
      return Description.NO_MATCH;
    }

    ExpressionTree workQueueExpressionTree = arguments.get(4);
    if (!UNBOUNDED_WORK_QUEUE_CONSTRUCTOR_MATCHER.matches(workQueueExpressionTree, state)) {
      return Description.NO_MATCH;
    }
    String maximumPoolSizeReplacement;
    // maximumPoolSize cannot be 0, so when corePoolSize is 0 set maximumPoolSize to 1 explicitly
    if (corePoolSize == 0) {
      maximumPoolSizeReplacement = "1";
    } else {
      maximumPoolSizeReplacement = state.getSourceForNode(arguments.get(0));
    }
    Fix maximumPoolSizeReplacementFix =
        SuggestedFix.builder().replace(arguments.get(1), maximumPoolSizeReplacement).build();
    Fix corePoolSizeReplacementFix =
        SuggestedFix.builder()
            .replace(arguments.get(0), state.getSourceForNode(arguments.get(1)))
            .build();
    return buildDescription(tree)
        .addFix(maximumPoolSizeReplacementFix)
        .addFix(corePoolSizeReplacementFix)
        .build();
  }
}
