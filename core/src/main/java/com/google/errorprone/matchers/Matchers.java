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

import com.google.errorprone.RefactoringVisitorState;
import com.google.errorprone.VisitorState;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import java.util.List;

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

  public static <T extends Tree> Matcher<T> anyOf(final Matcher<? super T>... matchers) {
    return new Matcher<T>() {
      @Override public boolean matches(T t, VisitorState state) {
        for (Matcher<? super T> matcher : matchers) {
          if (matcher.matches(t, state)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static <T extends Tree> Matcher<T> kindIs(final Kind kind) {
    return new Matcher<T>() {
      @Override public boolean matches(T tree, VisitorState state) {
        return tree.getKind() == kind;
      }
    };
  }

  public static <T extends Tree> Matcher<T> kindIs(final Kind kind, Class<T> typeInfer) {
    return new Matcher<T>() {
      @Override public boolean matches(T tree, VisitorState state) {
        return tree.getKind() == kind;
      }
    };
  }
  
  public static <T extends Tree> Matcher<T> isNull() {
    return new Matcher<T>() {
      @Override public boolean matches(T tree, VisitorState state) {
        return tree == null;
      }
    };
  }
  
  public static <T extends Tree> Matcher<T> isNull(Class<T> typeInfer) {
    return new Matcher<T>() {
      @Override public boolean matches(T tree, VisitorState state) {
        return tree == null;
      }
    };
  }

  public static StaticMethodMatcher staticMethod(String fullClassName, String methodName) {
    return new StaticMethodMatcher(fullClassName, methodName);
  }

  public static MethodInvocationMethodSelectMatcher methodSelect(
      Matcher<ExpressionTree> methodSelectMatcher) {
    return new MethodInvocationMethodSelectMatcher(methodSelectMatcher);
  }

  public static Matcher<ExpressionTree> expressionMethodSelect(Matcher<ExpressionTree> methodSelectMatcher) {
    return new ExpressionMethodSelectMatcher(methodSelectMatcher); 
  }
  
  public static Matcher<MethodInvocationTree> argument(
      final int position, final Matcher<ExpressionTree> argumentMatcher) {
    return new MethodInvocationArgumentMatcher(position, argumentMatcher);
  }

  public static <T extends Tree> Matcher<Tree> parentNode(Matcher<T> treeMatcher) {
    return new ParentNode<T>(treeMatcher);
  }

  public static <T extends Tree> Matcher<T> isSubtypeOf(Type type) {
    return new IsSubtypeOf<T>(type);
  }

  public static <T extends Tree> EnclosingBlock<T> enclosingBlock(Matcher<BlockTree> matcher) {
    return new EnclosingBlock<T>(matcher);
  }

  public static LastStatement lastStatement(Matcher<StatementTree> matcher) {
    return new LastStatement(matcher);
  }
  
  public static <T extends StatementTree> NextStatement<T> nextStatement(
      Matcher<StatementTree> matcher) {
    return new NextStatement<T>(matcher);
  }

  public static <T extends Tree> Same<T> same(T tree) {
    return new Same<T>(tree);
  }
  
  public static <T extends Tree> Matcher<T> not(final Matcher<T> matcher) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return !matcher.matches(t, state);
      }
    };
  }

  public static Matcher<ExpressionTree> stringLiteral(String value) {
    return new StringLiteralMatcher(value);
  }

  public static Matcher<AnnotationTree> hasElementWithValue(
      String element, Matcher<ExpressionTree> valueMatcher) {
    return new AnnotationHasElementWithValue(element, valueMatcher);
  }

  public static Matcher<AnnotationTree> isType(final String annotationClassName) {
    return new AnnotationTypeMatcher(annotationClassName);
  }

  public static Matcher<? super MethodInvocationTree> sameArgument(
      final int index1, final int index2) {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
        List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
        if (arguments.get(index1).getKind() == Kind.IDENTIFIER &&
            arguments.get(index2).getKind() == Kind.IDENTIFIER) {
          return ((JCIdent) arguments.get(index1)).sym == ((JCIdent) arguments.get(index2)).sym;
        }

        return false;
      }
    };
  }
}
