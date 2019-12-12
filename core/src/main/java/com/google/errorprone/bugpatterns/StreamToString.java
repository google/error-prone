/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "StreamToString",
    summary = "Calling toString on a Stream does not provide useful information",
    severity = ERROR)
public class StreamToString extends AbstractToString {

  private static final TypePredicate STREAM = isDescendantOf("java.util.stream.Stream");

  @Override
  protected TypePredicate typePredicate() {
    return STREAM;
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    return Optional.empty();
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    return Optional.empty();
  }
}
