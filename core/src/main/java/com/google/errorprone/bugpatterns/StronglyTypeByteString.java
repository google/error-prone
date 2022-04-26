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
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;

/**
 * Flags fields which would be better expressed as ByteStrings rather than primitive byte arrays.
 */
@BugPattern(
    summary =
        "This primitive byte array is only used to construct ByteStrings. It would be"
            + " clearer to strongly type the field instead.",
    severity = WARNING)
public final class StronglyTypeByteString extends BugChecker implements CompilationUnitTreeMatcher {

  private static final Matcher<ExpressionTree> BYTE_STRING_FACTORY =
      anyOf(
          staticMethod()
              .onClass("com.google.protobuf.ByteString")
              .namedAnyOf("copyFrom")
              .withParametersOfType(ImmutableSet.of(Suppliers.arrayOf(Suppliers.BYTE_TYPE))));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    return StronglyType.forCheck(this)
        .addType(state.arrayTypeForType(state.getSymtab().byteType))
        .setFactoryMatcher(BYTE_STRING_FACTORY)
        .build()
        .match(tree, state);
  }
}
