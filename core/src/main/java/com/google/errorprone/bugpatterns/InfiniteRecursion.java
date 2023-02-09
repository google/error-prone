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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;
import static com.sun.source.tree.Tree.Kind.CONDITIONAL_AND;
import static com.sun.source.tree.Tree.Kind.CONDITIONAL_OR;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "This method always recurses, and will cause a StackOverflowError",
    severity = ERROR)
public class InfiniteRecursion extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree declaration, VisitorState state) {
    if (declaration.getBody() == null) {
      return NO_MATCH;
    }
    MethodSymbol declaredSymbol = getSymbol(declaration);
    new SuppressibleTreePathScanner<Void, Boolean>(state) {
      /*
       * Once we hit a `return`, we know that any code afterward is not necessarily always executed.
       * So we track that and stop reporting infinite recursion afterward.
       */

      boolean mayHaveReturned;

      @Override
      public Void visitReturn(ReturnTree tree, Boolean underConditional) {
        super.visitReturn(tree, underConditional);
        mayHaveReturned = true;
        return null;
      }

      /*
       * Some other code is executed only conditionally. We need to scan such code to look for
       * `return` statements, but we don't report infinite recursion for any self-calls there. We
       * track this sort of conditional-ness through the scanner's Boolean parameter.
       *
       * Since we're scanning such code only for `return` statements, there's no need to scan any
       * code that contains only expressions (e.g., for a ternary, getTrueExpression()
       * and getFalseExpression()).
       */

      @Override
      public Void visitBinary(BinaryTree tree, Boolean underConditional) {
        scan(tree.getLeftOperand(), underConditional);
        if (tree.getKind() != CONDITIONAL_AND && tree.getKind() != CONDITIONAL_OR) {
          scan(tree.getRightOperand(), underConditional);
        } // otherwise, no need to conditionally visit getRightOperand() expression
        return null;
      }

      @Override
      public Void visitCase(CaseTree tree, Boolean underConditional) {
        return super.visitCase(tree, /*underConditional*/ true);
      }

      @Override
      public Void visitCatch(CatchTree tree, Boolean underConditional) {
        /*
         * We mostly ignore exceptions. Notably, if a loop would be infinite *except* that it throws
         * an exception, we still report it as an infinite loop. But we do want to consider `catch`
         * blocks to be conditionally executed, since it would be reasonable for a method to
         * delegate to itself (on another object or with a different argument) in case of an
         * exception. For example, `toString(COMPLEX)` might fall back to `toString(SIMPLE)`.
         */
        return super.visitCatch(tree, /*underConditional*/ true);
      }

      @Override
      public Void visitConditionalExpression(
          ConditionalExpressionTree tree, Boolean underConditional) {
        scan(tree.getCondition(), underConditional);
        // no need to conditionally visit getTrueExpression() and getFalseExpression() expressions
        return null;
      }

      @Override
      public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Boolean underConditional) {
        scan(tree.getExpression(), underConditional);
        scan(tree.getStatement(), /*underConditional*/ true);
        return null;
      }

      @Override
      public Void visitForLoop(ForLoopTree tree, Boolean underConditional) {
        scan(tree.getInitializer(), underConditional);
        scan(tree.getCondition(), underConditional);
        scan(tree.getStatement(), /*underConditional*/ true);
        // no need to conditionally visit getUpdate() expressions
        return null;
      }

      @Override
      public Void visitIf(IfTree tree, Boolean underConditional) {
        scan(tree.getCondition(), underConditional);
        scan(tree.getThenStatement(), /*underConditional*/ true);
        scan(tree.getElseStatement(), /*underConditional*/ true);
        return null;
      }

      @Override
      public Void visitWhileLoop(WhileLoopTree tree, Boolean underConditional) {
        scan(tree.getCondition(), underConditional);
        scan(tree.getStatement(), /*underConditional*/ true);
        return null;
      }

      /*
       * Don't descend into classes and lambdas at all, but resume checking afterward.
       *
       * We neither want to report supposedly "recursive" calls inside them nor scan them for
       * `return` (which would cause us to stop scanning entirely).
       */

      @Override
      public Void visitClass(ClassTree tree, Boolean underConditional) {
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree tree, Boolean underConditional) {
        return null;
      }

      // Finally, the thing we're actually looking for:

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Boolean underConditional) {
        checkInvocation(tree, underConditional);
        return super.visitMethodInvocation(tree, underConditional);
      }

      // Handling constructors doesn't appear worthwhile: See b/264529494#comment11 and afterward.

      void checkInvocation(MethodInvocationTree invocation, boolean underConditional) {
        if (mayHaveReturned || underConditional) {
          return;
        }
        if (!declaredSymbol.equals(getSymbol(invocation))) {
          return;
        }
        /*
         * We have provably infinite recursion if the call goes to the same implementation code that
         * we're analyzing. We already know that we're looking at the same MethodSymbol, so now we
         * just need to make sure that the call isn't a virtual call that may resolve to a different
         * implementation. Specifically, we can resolve the call as triggering infinite recursion in
         * the case of any non-overridable method (including a static method) and of any call on
         * this object.
         */
        if (!methodCanBeOverridden(declaredSymbol) || isCallOnThisObject(invocation)) {
          state.reportMatch(describeMatch(invocation));
        }
      }
    }.scan(new TreePath(state.getPath(), declaration.getBody()), /*underConditional*/ false);

    return NO_MATCH; // We reported any matches through state.reportMatch.
  }

  private static boolean isCallOnThisObject(MethodInvocationTree invocation) {
    ExpressionTree select = invocation.getMethodSelect();
    return select.getKind() == IDENTIFIER || isThis(((MemberSelectTree) select).getExpression());
  }

  // TODO(b/236055787): Share code with various checks that look for "return this."
  private static boolean isThis(ExpressionTree input) {
    return new SimpleTreeVisitor<Boolean, Void>() {
      @Override
      public Boolean visitParenthesized(ParenthesizedTree tree, Void unused) {
        return visit(tree.getExpression(), null);
      }

      @Override
      public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
        return visit(tree.getExpression(), null);
      }

      @Override
      public Boolean visitMemberSelect(MemberSelectTree tree, Void unused) {
        /*
         * The caller has already checked that the MethodSymbol is from this class, so we don't need
         * to disambiguate between ThisClass.this and SomeOtherEnclosingClass.this.
         */
        return tree.getIdentifier().contentEquals("this");
      }

      @Override
      public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
        return tree.getName().contentEquals("this");
      }

      @Override
      protected Boolean defaultAction(Tree tree, Void unused) {
        return false;
      }
    }.visit(input, null);
  }
}
