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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.TrustingNullnessAnalysis;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import java.util.Optional;

/**
 * Flags methods with collection return types which return {@link null} in some cases but don't
 * annotate the method as @Nullable.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "ReturnsNullCollection",
    summary =
        "Methods has a collection return type and returns {@link null} in some cases but does not"
            + " annotate the method as @Nullable. See Effective Java 3rd Edition Item 54.",
    severity = SUGGESTION)
public class ReturnsNullCollection extends AbstractMethodReturnsNull {

  private static boolean methodWithoutNullable(MethodTree tree, VisitorState state) {
    return !TrustingNullnessAnalysis.hasNullableAnnotation(getSymbol(tree));
  }

  private static final Matcher<MethodTree> METHOD_RETURNS_COLLECTION_WITHOUT_NULLABLE_ANNOTATION =
      allOf(
          anyOf(
              methodReturns(isSubtypeOf("java.util.Collection")),
              methodReturns(isSubtypeOf("java.util.Map"))),
          ReturnsNullCollection::methodWithoutNullable);

  public ReturnsNullCollection() {
    super(METHOD_RETURNS_COLLECTION_WITHOUT_NULLABLE_ANNOTATION);
  }

  @Override
  protected Optional<Fix> provideFix(ReturnTree tree) {
    return Optional.empty();
  }
}
