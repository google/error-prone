/*
 * Copyright 2012 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Optional;
import javax.inject.Inject;

/**
 * @author adgar@google.com (Mike Edgar)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    summary = "Calling toString on an array does not provide useful information",
    severity = ERROR)
public class ArrayToString extends AbstractToString {

  private static final Matcher<ExpressionTree> GET_STACK_TRACE =
      instanceMethod().onDescendantOf("java.lang.Throwable").named("getStackTrace");

  private static final TypePredicate IS_ARRAY = TypePredicates.isArray();

  @Inject
  ArrayToString(ErrorProneFlags flags) {
    super(flags);
  }

  @Override
  protected TypePredicate typePredicate() {
    return IS_ARRAY;
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree stringifiedExpr, VisitorState state) {
    // e.g. println(theArray) -> println(Arrays.toString(theArray))
    // or:  "" + theArray -> "" + Arrays.toString(theArray)
    return toStringFix(stringifiedExpr, stringifiedExpr, state);
  }

  @Override
  protected boolean allowableToStringKind(ToStringKind toStringKind) {
    return toStringKind == ToStringKind.FLOGGER || toStringKind == ToStringKind.FORMAT_METHOD;
  }

  @Override
  protected Optional<Fix> toStringFix(
      Tree toStringCall, ExpressionTree stringifiedExpr, VisitorState state) {
    // If the array is the result of calling e.getStackTrace(), replace
    // e.getStackTrace().toString() with Guava's Throwables.getStackTraceAsString(e).
    if (GET_STACK_TRACE.matches(stringifiedExpr, state)) {
      var fix = SuggestedFix.builder();
      return Optional.of(
          fix.replace(
                  toStringCall,
                  String.format(
                      "%s.getStackTraceAsString(%s)",
                      qualifyType(state, fix, "com.google.common.base.Throwables"),
                      state.getSourceForNode(getReceiver(stringifiedExpr))))
              .build());
    }
    // e.g. String.valueOf(theArray) -> Arrays.toString(theArray)
    // or:  theArray.toString() -> Arrays.toString(theArray)
    // or:  theArrayOfArrays.toString() -> Arrays.deepToString(theArrayOfArrays)
    return fix(toStringCall, stringifiedExpr, state);
  }

  @Override
  protected Optional<Fix> memberReferenceFix(
      MemberReferenceTree tree, Type receiverType, VisitorState state) {
    String method = isNestedArray(receiverType, state) ? "deepToString" : "toString";
    var fix = SuggestedFix.builder();
    return Optional.of(
        fix.replace(tree, qualifyType(state, fix, "java.util.Arrays") + "::" + method).build());
  }

  private static Optional<Fix> fix(Tree toStringCall, Tree stringifiedExpr, VisitorState state) {
    String method = isNestedArray(getType(stringifiedExpr), state) ? "deepToString" : "toString";
    var fix = SuggestedFix.builder();
    return Optional.of(
        fix.replace(
                toStringCall,
                String.format(
                    "%s.%s(%s)",
                    qualifyType(state, fix, "java.util.Arrays"),
                    method,
                    state.getSourceForNode(stringifiedExpr)))
            .build());
  }

  private static boolean isNestedArray(Type type, VisitorState state) {
    Types types = state.getTypes();
    return type != null && types.isArray(type) && types.isArray(types.elemtype(type));
  }
}
