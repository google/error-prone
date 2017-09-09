/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.argumentCount;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnionType;

/** Catches no-op calls to {@code Throwables.throwIfUnchecked}. */
@BugPattern(
  name = "ThrowIfUncheckedKnownChecked",
  summary = "throwIfUnchecked(knownCheckedException) is a no-op.",
  explanation =
      "`throwIfUnchecked(knownCheckedException)` is a no-op (aside from performing a null check). "
          + "`propagateIfPossible(knownCheckedException)` is a complete no-op.",
  category = GUAVA,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ThrowIfUncheckedKnownChecked extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> IS_THROW_IF_UNCHECKED =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Throwables").named("throwIfUnchecked"),
              staticMethod()
                  .onClass("com.google.common.base.Throwables")
                  .named("propagateIfPossible")),
          argumentCount(1));

  private static final Matcher<ExpressionTree> IS_KNOWN_CHECKED_EXCEPTION =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Type type = ((JCTree) tree).type;
          if (type.isUnion()) {
            for (TypeMirror alternative : ((UnionType) type).getAlternatives()) {
              if (!isKnownCheckedException(state, (Type) alternative)) {
                return false;
              }
            }
            return true;
          } else {
            return isKnownCheckedException(state, type);
          }
        }

        boolean isKnownCheckedException(VisitorState state, Type type) {
          Types types = state.getTypes();
          Symtab symtab = state.getSymtab();
          // Check erasure for generics.
          type = types.erasure(type);
          return
          // Has to be some Exception: A variable of type Throwable might be an Error.
          types.isSubtype(type, symtab.exceptionType)
              // Has to be some subtype: A variable of type Exception might be a RuntimeException.
              && !types.isSameType(type, symtab.exceptionType)
              // Can't be of type RuntimeException.
              && !types.isSubtype(type, symtab.runtimeExceptionType);
        }
      };

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (IS_THROW_IF_UNCHECKED.matches(tree, state)
        && argument(0, IS_KNOWN_CHECKED_EXCEPTION).matches(tree, state)) {
      return describeMatch(tree, delete(state.getPath().getParentPath().getLeaf()));
    }
    return NO_MATCH;
  }
}
