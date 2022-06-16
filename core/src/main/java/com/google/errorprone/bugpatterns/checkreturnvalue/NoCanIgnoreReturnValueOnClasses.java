/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checker that "pushes" the {@code @CanIgnoreReturnValue} annotation down from classes to methods.
 */
@BugPattern(
    summary =
        "@CanIgnoreReturnValue should not be applied to classes as it almost always overmatches (as"
            + " it applies to constructors and all methods), and the CIRVness isn't conferred to"
            + " its subclasses.",
    severity = ERROR)
public final class NoCanIgnoreReturnValueOnClasses extends BugChecker implements ClassTreeMatcher {
  private static final String CRV = "com.google.errorprone.annotations.CheckReturnValue";
  private static final String CIRV = "com.google.errorprone.annotations.CanIgnoreReturnValue";

  private static final String EXTRA_SUFFIX = "";

  @VisibleForTesting
  static final String METHOD_COMMENT = " // pushed down from class to method;" + EXTRA_SUFFIX;

  @VisibleForTesting
  static final String CTOR_COMMENT = " // pushed down from class to constructor;" + EXTRA_SUFFIX;

  private static final MultiMatcher<ClassTree, AnnotationTree> HAS_CIRV_ANNOTATION =
      annotations(AT_LEAST_ONE, isType(CIRV));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    MultiMatchResult<AnnotationTree> cirvAnnotation =
        HAS_CIRV_ANNOTATION.multiMatchResult(tree, state);
    // if the class isn't directly annotated w/ @CIRV, bail out
    if (!cirvAnnotation.matches()) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    String cirvName = qualifyType(state, fix, CIRV);

    // remove @CIRV from the class
    fix.delete(cirvAnnotation.onlyMatchingNode());

    // theoretically, we could also add @CRV to the class, since all APIs will have CIRV pushed down
    // onto them, but it's very likely that a larger enclosing scope will already be @CRV (otherwise
    // why did the user annotate this class as @CIRV?)

    // scan the tree and add @CIRV to all non-void method declarations that aren't already annotated
    // with @CIRV or @CRV
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        // stop descending when we reach a class that's marked @CRV
        return hasAnnotation(classTree, CRV, state) ? null : super.visitClass(classTree, unused);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        if (shouldAddCirv(methodTree, state)) {
          String trailingComment = null;

          if (methodTree.getReturnType() == null) { // constructor
            trailingComment = CTOR_COMMENT;
          } else if (alwaysReturnsThis()) {
            trailingComment = "";
          } else {
            trailingComment = METHOD_COMMENT;
          }

          fix.prefixWith(methodTree, "@" + cirvName + trailingComment + "\n");
        }
        // TODO(kak): we could also consider removing CRV from individual methods (since the
        // enclosing class is now annotated as CRV.
        return null;
      }

      private boolean alwaysReturnsThis() {
        // TODO(b/236055787): share this TreePathScanner
        AtomicBoolean allReturnThis = new AtomicBoolean(true);
        AtomicBoolean atLeastOneReturn = new AtomicBoolean(false);

        new TreePathScanner<Void, Void>() {
          private final Set<VarSymbol> thises = new HashSet<>();

          @Override
          public Void visitVariable(VariableTree variableTree, Void unused) {
            VarSymbol symbol = getSymbol(variableTree);
            if (isConsideredFinal(symbol) && maybeCastThis(variableTree.getInitializer())) {
              thises.add(symbol);
            }
            return super.visitVariable(variableTree, null);
          }

          @Override
          public Void visitReturn(ReturnTree returnTree, Void unused) {
            atLeastOneReturn.set(true);
            if (!isThis(returnTree.getExpression())) {
              allReturnThis.set(false);
              // once we've set allReturnThis to false, no need to descend further
              return null;
            }
            return super.visitReturn(returnTree, null);
          }

          /** Returns whether the given {@link ExpressionTree} is {@code this}. */
          private boolean isThis(ExpressionTree returnExpression) {
            return maybeCastThis(returnExpression) || thises.contains(getSymbol(returnExpression));
          }

          @Override
          public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            // don't descend into lambdas
            return null;
          }

          @Override
          public Void visitNewClass(NewClassTree node, Void unused) {
            // don't descend into declarations of anonymous classes
            return null;
          }

          private boolean maybeCastThis(Tree tree) {
            return firstNonNull(
                new SimpleTreeVisitor<Boolean, Void>() {

                  @Override
                  public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
                    return visit(tree.getExpression(), null);
                  }

                  @Override
                  public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
                    return tree.getName().contentEquals("this");
                  }

                  @Override
                  public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                    return getSymbol(tree).getSimpleName().contentEquals("self");
                  }
                }.visit(tree, null),
                false);
          }
        }.scan(getCurrentPath(), null);

        return allReturnThis.get() && atLeastOneReturn.get();
      }

      private boolean shouldAddCirv(MethodTree methodTree, VisitorState state) {
        if (isVoidType(getType(methodTree.getReturnType()), state)) { // void return types
          return false;
        }
        if (hasAnnotation(methodTree, CIRV, state)) {
          return false;
        }
        if (hasAnnotation(methodTree, CRV, state)) {
          return false;
        }
        // if the constructor is implicit, don't add CIRV (we can't annotate a synthetic node!)
        if (isGeneratedConstructor(methodTree)) {
          return false;
        }
        // if the method is inside an AV or AV.Builder and is abstract (no body), don't add CIRV
        ClassSymbol enclosingClass = enclosingClass(getSymbol(methodTree));
        if (hasAnnotation(enclosingClass, "com.google.auto.value.AutoValue", state)
            || hasAnnotation(enclosingClass, "com.google.auto.value.AutoValue.Builder", state)) {
          if (methodTree.getBody() == null) {
            return false;
          }
        }
        // TODO(kak): should we also return false for private methods? I'm betting most of them are
        // "accidentally" CIRV'ed by the enclosing class; any compile errors would be caught by
        // building the enclosing class anyways.
        return true;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        // don't descend into lambdas
        return null;
      }

      @Override
      public Void visitNewClass(NewClassTree node, Void unused) {
        // don't descend into declarations of anonymous classes
        return null;
      }
    }.scan(state.getPath(), null);
    return describeMatch(tree, fix.build());
  }
}
