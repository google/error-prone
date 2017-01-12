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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Checks if a constructor or method annotated with {@link MustBeClosed} is called within the
 * resource variable initializer of a try-with-resources statement.
 */
@BugPattern(
  name = "MustBeClosedChecker",
  summary =
      "Invocations of methods or constructors annotated with @MustBeClosed must occur within"
          + " the resource variable initializer of a try-with-resources statement.",
  explanation =
      "Methods or constructors annotated with @MustBeClosed require that the returned"
          + " resource is closed. This is enforced by checking that invocations occur"
          + " within the resource variable initializer of a try-with-resources statement.",
  category = JDK,
  severity = ERROR
)
public class MustBeClosedChecker extends BugChecker
    implements MethodTreeMatcher, MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<Tree> HAS_MUST_BE_CLOSED_ANNOTATION =
      hasAnnotation(MustBeClosed.class.getCanonicalName());

  private static final Matcher<MethodTree> METHOD_RETURNS_AUTO_CLOSEABLE_MATCHER =
      allOf(not(methodIsConstructor()), methodReturns(isSubtypeOf("java.lang.AutoCloseable")));

  private static final Matcher<MethodTree> AUTO_CLOSEABLE_CONSTRUCTOR_MATCHER =
      allOf(methodIsConstructor(), enclosingClass(isSubtypeOf("java.lang.AutoCloseable")));

  /**
   * Check that the {@link MustBeClosed} annotation is only used for constructors of AutoCloseables
   * and methods that return an AutoCloseable.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(tree, state)) {
      // Ignore methods and constructors that are not annotated with {@link MustBeClosed}.
      return NO_MATCH;
    }

    boolean isAConstructor = methodIsConstructor().matches(tree, state);
    if (isAConstructor && !AUTO_CLOSEABLE_CONSTRUCTOR_MATCHER.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage("MustBeClosed should only annotate constructors of AutoCloseables.")
          .build();
    }

    if (!isAConstructor && !METHOD_RETURNS_AUTO_CLOSEABLE_MATCHER.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage("MustBeClosed should only annotate methods that return an AutoCloseable.")
          .build();
    }
    return NO_MATCH;
  }

  /**
   * Check that invocations of methods annotated with {@link MustBeClosed} are called within the
   * resource variable initializer of a try-with-resources statement.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    String methodName = getSymbol(tree).getSimpleName().toString();
    return matchNewClassOrMethodInvocation(methodName, tree, state);
  }

  /**
   * Check that construction of constructors annotated with {@link MustBeClosed} occurs within the
   * resource variable initializer of a try-with-resources statement.
   */
  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchNewClassOrMethodInvocation("Constructor", tree, state);
  }

  /**
   * Check that constructors and methods annotated with {@link MustBeClosed} occur within the
   * resource variable initializer of a try-with-resources statement.
   *
   * @param name The name of the method or constructor.
   */
  private Description matchNewClassOrMethodInvocation(String name, Tree tree, VisitorState state) {
    if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(tree, state)) {
      // Ignore invocations of methods and constructors that are not annotated with
      // {@link MustBeClosed}.
      return NO_MATCH;
    }

    if (getSymbol(state.findEnclosing(ClassTree.class)).equals(getSymbol(tree).enclClass())) {
      // Do not enforce the check for uses of the annotated method or constructor that occur in
      // the same class that defines them.
      return NO_MATCH;
    }

    String errorMessage =
        String.format(
            "%s must be called within the resource variable initializer of a try-with-resources"
                + " statement.",
            name);

    MethodTree callerMethodTree = enclosingMethod(state);
    if (state.getPath().getParentPath().getLeaf().getKind() == Tree.Kind.RETURN
        && callerMethodTree != null) {
      // The invocation occurs within a return statement of a method, instead of a lambda
      // expression or anonymous class.

      if (HAS_MUST_BE_CLOSED_ANNOTATION.matches(callerMethodTree, state)) {
        // Ignore invocations of annotated methods and constructors that occur in the return
        // statement of an annotated caller method, since invocations of the caller are enforced.
        return NO_MATCH;
      }

      // The caller method is not annotated, so the closing of the returned resource is not
      // enforced. Suggest fixing this by annotating the caller method.
      return buildDescription(tree)
          .addFix(
              SuggestedFix.builder()
                  .prefixWith(callerMethodTree, "@MustBeClosed\n")
                  .addImport(MustBeClosed.class.getCanonicalName())
                  .build())
          .setMessage(errorMessage)
          .build();
    }

    if (!inTWR(state)) {
      // The constructor or method invocation does not occur within the resource variable
      // initializer of a try-with-resources statement.
      return buildDescription(tree).setMessage(errorMessage).build();
    }
    return NO_MATCH;
  }

  /**
   * Returns the enclosing method of the given visitor state. Returns null if the state is within a
   * lambda expression or anonymous class.
   */
  @Nullable
  private static MethodTree enclosingMethod(VisitorState state) {
    for (Tree node : state.getPath().getParentPath()) {
      switch (node.getKind()) {
        case LAMBDA_EXPRESSION:
        case NEW_CLASS:
          return null;
        case METHOD:
          return (MethodTree) node;
        default:
          break;
      }
    }
    return null;
  }

  /**
   * Returns whether an invocation occurs within the resource variable initializer of a
   * try-with-resources statement.
   */
  // TODO(cushon): This method has been copied from FilesLinesLeak. Move it to a shared class.
  private boolean inTWR(VisitorState state) {
    TreePath path = state.getPath().getParentPath();
    while (path.getLeaf().getKind() == Tree.Kind.CONDITIONAL_EXPRESSION) {
      path = path.getParentPath();
    }
    Symbol sym = getSymbol(path.getLeaf());
    return sym != null && sym.getKind() == ElementKind.RESOURCE_VARIABLE;
  }
}
