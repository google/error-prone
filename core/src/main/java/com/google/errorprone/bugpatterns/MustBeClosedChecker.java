/*
 * Copyright 2016 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.AnnotationNames.MUST_BE_CLOSED_ANNOTATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;

/**
 * Checks if a constructor or method annotated with {@link
 * com.google.errorprone.annotations.MustBeClosed} is called within the resource variable
 * initializer of a try-with-resources statement.
 */
@BugPattern(
    altNames = "MustBeClosed",
    summary =
        "This method returns a resource which must be managed carefully, not just left for garbage"
            + " collection. If it is a constant that will persist for the lifetime of your"
            + " program, move it to a private static final field. Otherwise, you should use it in"
            + " a try-with-resources.",
    severity = ERROR)
public class MustBeClosedChecker extends AbstractMustBeClosedChecker
    implements MethodTreeMatcher, ClassTreeMatcher {

  private static final Matcher<Tree> IS_AUTOCLOSEABLE = isSubtypeOf(AutoCloseable.class);

  private static final Matcher<MethodTree> METHOD_RETURNS_AUTO_CLOSEABLE_MATCHER =
      allOf(not(methodIsConstructor()), methodReturns(IS_AUTOCLOSEABLE));

  private static final Matcher<MethodTree> AUTO_CLOSEABLE_CONSTRUCTOR_MATCHER =
      allOf(methodIsConstructor(), enclosingClass(isSubtypeOf(AutoCloseable.class)));

  /**
   * Check that the {@code MustBeClosed} annotation is only used for constructors of AutoCloseables
   * and methods that return an AutoCloseable.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    // Scan the whole method at once, to avoid name clashes for resource variables.
    state.reportMatch(
        scanEntireMethodFor(
            (t, s) -> {
              if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(t, s)) {
                return false;
              }
              if (t instanceof MethodInvocationTree && ASTHelpers.getSymbol(t).isConstructor()) {
                // Invocations of constructors, like `this()` and `super()`, act kinda weird.
                // they're handled specially in matchClass, and should be ignored here.
                return false;
              }
              return true;
            },
            tree,
            state));
    if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(tree, state)) {
      // But otherwise ignore methods and constructors that are not annotated with {@link
      // MustBeClosed}.
      return NO_MATCH;
    }

    // If the method/constructor is annotated @MBC, make sure it's a valid annotation.
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

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!IS_AUTOCLOSEABLE.matches(tree, state)) {
      return NO_MATCH;
    }

    // Find all of the constructors in the class without the {@code @MustBeClosed} annotation, which
    // invoke a constructor with {@code MustBeClosed}.
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree methodTree)) {
        continue;
      }

      if (!ASTHelpers.getSymbol(methodTree).isConstructor()
          || hasAnnotation(methodTree, MUST_BE_CLOSED_ANNOTATION, state)
          || !invokedConstructorMustBeClosed(state, methodTree)) {
        continue;
      }

      if (ASTHelpers.isGeneratedConstructor(methodTree)) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(
                    "Implicitly invoked constructor is marked @MustBeClosed, so this class must "
                        + "have an explicit constructor with @MustBeClosed also.")
                .build());
      } else {
        SuggestedFix.Builder builder = SuggestedFix.builder();
        String suggestedFixName =
            SuggestedFixes.qualifyType(
                state, builder, state.getTypeFromString(MUST_BE_CLOSED_ANNOTATION));
        SuggestedFix fix = builder.prefixWith(methodTree, "@" + suggestedFixName + " ").build();

        state.reportMatch(
            buildDescription(methodTree)
                .addFix(fix)
                .setMessage(
                    "Invoked constructor is marked @MustBeClosed, so this constructor must be "
                        + "marked @MustBeClosed too.")
                .build());
      }
    }

    return NO_MATCH;
  }

  private static boolean invokedConstructorMustBeClosed(VisitorState state, MethodTree methodTree) {
    // The first statement in a constructor should be an invocation of the super/this constructor.
    List<? extends StatementTree> statements = methodTree.getBody().getStatements();
    if (statements.isEmpty()) {
      // Not sure how the body would be empty, but just filter it out in case.
      return false;
    }
    ExpressionStatementTree est = (ExpressionStatementTree) statements.get(0);
    MethodInvocationTree mit = (MethodInvocationTree) est.getExpression();
    MethodSymbol invokedConstructorSymbol = ASTHelpers.getSymbol(mit);
    return hasAnnotation(invokedConstructorSymbol, MUST_BE_CLOSED_ANNOTATION, state);
  }
}
