/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.stream.Collectors;

/**
 * Value class for holding suggested changes to method call arguments.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@AutoValue
abstract class Changes {

  abstract ImmutableList<Double> originalCost();

  abstract ImmutableList<Double> assignmentCost();

  abstract ImmutableList<ParameterPair> changedPairs();

  boolean isEmpty() {
    return changedPairs().isEmpty();
  }

  double totalAssignmentCost() {
    return assignmentCost().stream().mapToDouble(d -> d).sum();
  }

  double totalOriginalCost() {
    return originalCost().stream().mapToDouble(d -> d).sum();
  }

  static Changes create(
      ImmutableList<Double> originalCost,
      ImmutableList<Double> assignmentCost,
      ImmutableList<ParameterPair> changedPairs) {
    return new AutoValue_Changes(originalCost, assignmentCost, changedPairs);
  }

  static Changes empty() {
    return new AutoValue_Changes(ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
  }

  SuggestedFix buildCommentArgumentsFix(InvocationInfo info) {
    SuggestedFix.Builder commentArgumentsFixBuilder = SuggestedFix.builder();
    for (ParameterPair change : changedPairs()) {
      int index = change.formal().index();
      ExpressionTree actual = info.actualParameters().get(index);
      int startPosition = ((JCTree) actual).getStartPosition();
      String formal = info.formalParameters().get(index).getSimpleName().toString();
      commentArgumentsFixBuilder.replace(
          startPosition, startPosition, NamedParameterComment.toCommentText(formal));
    }
    return commentArgumentsFixBuilder.build();
  }

  SuggestedFix buildPermuteArgumentsFix(InvocationInfo info) {
    SuggestedFix.Builder permuteArgumentsFixBuilder = SuggestedFix.builder();
    for (ParameterPair pair : changedPairs()) {
      permuteArgumentsFixBuilder.replace(
          info.actualParameters().get(pair.formal().index()),
          // use getSourceForNode to avoid javac pretty printing the replacement (pretty printing
          // converts unicode characters to unicode escapes)
          info.state().getSourceForNode(info.actualParameters().get(pair.actual().index())));
    }
    return permuteArgumentsFixBuilder.build();
  }

  public String describe(InvocationInfo info) {
    return "The following arguments may have been swapped: "
        + changedPairs().stream()
            .map(
                p ->
                    String.format(
                        "'%s' for formal parameter '%s'",
                        info.state()
                            .getSourceForNode(info.actualParameters().get(p.formal().index())),
                        p.formal().name()))
            .collect(Collectors.joining(", "))
        + ". Either add clarifying `/* paramName= */` comments, or swap the arguments if that is"
        + " what was intended";
  }
}
