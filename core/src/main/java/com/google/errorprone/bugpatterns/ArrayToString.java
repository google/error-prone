/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

/**
 * @author adgar@google.com (Mike Edgar)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "ArrayToString",
  summary = "Calling toString on an array does not provide useful information",
  explanation =
      "The `toString` method on an array will print its identity, such as `[I@4488aabb`. This "
          + "is almost never needed. Use `Arrays.toString` to print a human-readable summary.",
  category = JDK,
  severity = ERROR
)
public class ArrayToString extends AbstractToString {

  private static final Matcher<ExpressionTree> GET_STACK_TRACE =
      instanceMethod().onDescendantOf("java.lang.Throwable").named("getStackTrace");

  private static final TypePredicate IS_ARRAY = TypePredicates.isArray();

  @Override
  protected TypePredicate typePredicate() {
    return IS_ARRAY;
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    // e.g. println(theArray) -> println(Arrays.toString(theArray))
    // or:  "" + theArray -> "" + Arrays.toString(theArray)
    return fix(tree, tree, state);
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    // If the array is the result of calling e.getStackTrace(), replace
    // e.getStackTrace().toString() with Guava's Throwables.getStackTraceAsString(e).
    if (GET_STACK_TRACE.matches(tree, state)) {
      return Optional.of(
          SuggestedFix.builder()
              .addImport("com.google.common.base.Throwables")
              .replace(
                  parent,
                  String.format(
                      "Throwables.getStackTraceAsString(%s)",
                      state.getSourceForNode(ASTHelpers.getReceiver(tree))))
              .build());
    }
    // e.g. String.valueOf(theArray) -> Arrays.toString(theArray)
    // or:  theArray.toString() -> Arrays.toString(theArray)
    return fix(parent, tree, state);
  }

  private Optional<Fix> fix(Tree replace, Tree with, VisitorState state) {
    return Optional.of(
        SuggestedFix.builder()
            .addImport("java.util.Arrays")
            .replace(replace, String.format("Arrays.toString(%s)", state.getSourceForNode(with)))
            .build());
  }
}
