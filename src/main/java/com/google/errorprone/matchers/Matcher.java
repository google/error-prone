/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

import java.lang.Thread.State;

/**
 * An embedded predicate DSL for matching Java source code.
 * @author alexeagle@google.com (Alex Eagle)
 */
public abstract class Matcher<T extends Tree> {
  public abstract boolean matches(T t, VisitorState state);

  <T extends Tree> Matcher<T> allOf(
      final Matcher<? super T>... matchers) {
    return new Matcher<T>() {
      @Override public boolean matches(T t, VisitorState state) {
        for (Matcher<? super T> matcher : matchers) {
          if (!matcher.matches(t, state)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  <T extends Tree> Matcher<T> capture(
      final com.google.errorprone.matchers.CapturingMatcher.TreeHolder holder, final Matcher<T> matcher) {
    return new CapturingMatcher<T>(matcher, holder);
  }

  Matcher<ExpressionTree> kindOf(final Kind kind) {
    return new Matcher<ExpressionTree>() {
      @Override public boolean matches(ExpressionTree tree, VisitorState state) {
        return tree.getKind() == kind;
      }
    };
  }

  StaticMethodMatcher staticMethod(String packageName, String className, String methodName) {
    return new StaticMethodMatcher(packageName, className, methodName);
  }

  MethodInvocationMethodSelectMatcher methodSelect(
      Matcher<ExpressionTree> methodSelectMatcher) {
    return new MethodInvocationMethodSelectMatcher(methodSelectMatcher);
  }

  Matcher<MethodInvocationTree> argument(
      final int position, final Matcher<ExpressionTree> argumentMatcher) {
    return new MethodInvocationArgumentMatcher(position, argumentMatcher);
  }

}
