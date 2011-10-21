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
import com.sun.tools.javac.code.Type;

/**
 * Static factory methods which make the DSL read better.
 *
 * TODO: it's probably worth the optimization to keep a single instance of each Matcher, rather than
 * create new instances each time the static method is called.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Matchers {
  private Matchers() {}


  public static <T extends Tree> Matcher<T> allOf(final Matcher<? super T>... matchers) {
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

  public static <T extends Tree> Matcher<T> capture(
      final CapturingMatcher.TreeHolder holder, final Matcher<T> matcher) {
    return new CapturingMatcher<T>(matcher, holder);
  }

  public static Matcher<ExpressionTree> kindOf(final Kind kind) {
    return new Matcher<ExpressionTree>() {
      @Override public boolean matches(ExpressionTree tree, VisitorState state) {
        return tree.getKind() == kind;
      }
    };
  }

  public static StaticMethodMatcher staticMethod(String packageName, String className, String methodName) {
    return new StaticMethodMatcher(packageName, className, methodName);
  }

  public static MethodInvocationMethodSelectMatcher methodSelect(
      Matcher<ExpressionTree> methodSelectMatcher) {
    return new MethodInvocationMethodSelectMatcher(methodSelectMatcher);
  }

  public static Matcher<MethodInvocationTree> argument(
      final int position, final Matcher<ExpressionTree> argumentMatcher) {
    return new MethodInvocationArgumentMatcher(position, argumentMatcher);
  }

  public static Matcher<Tree> parentNodeIs(Kind kind) {
    return new ParentNodeIs(kind);
  }

  public static <T extends Tree> Matcher<T> not(Matcher<T> matcher) {
    return new Not<T>(matcher);
  }

  public static <T extends Tree> Matcher<T> isSubtypeOf(Type type) {
    return new IsSubtypeOf<T>(type);
  }
}
