/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "StreamToString",
  summary = "Calling toString on a Stream does not provide useful information",
  explanation =
      "The `toString` method on a `Stream` will print its identity, such as "
          + "`java.util.stream.ReferencePipeline$Head@6d06d69c`. This is rarely what was intended.",
  category = JDK,
  severity = ERROR
)
public class StreamToString extends AbstractToString {

  static final TypePredicate STREAM =
      new TypePredicate() {
        @Override
        public boolean apply(Type type, VisitorState state) {
          Type stream = state.getTypeFromString("java.util.stream.Stream");
          return ASTHelpers.isSubtype(type, stream, state);
        }
      };

  @Override
  protected TypePredicate typePredicate() {
    return STREAM;
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    return Optional.absent();
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    return Optional.absent();
  }
}
