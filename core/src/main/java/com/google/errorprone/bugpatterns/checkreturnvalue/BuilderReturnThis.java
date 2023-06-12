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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.lang.Boolean.TRUE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** Discourages builder instance methods that do not return 'this'. */
@BugPattern(summary = "Builder instance method does not return 'this'", severity = WARNING)
public class BuilderReturnThis extends BugChecker implements MethodTreeMatcher {

  private static final String CRV = "com.google.errorprone.annotations.CheckReturnValue";

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol sym = getSymbol(tree);
    if (tree.getBody() == null) {
      return NO_MATCH;
    }
    if (!instanceReturnsBuilder(sym, state)) {
      return NO_MATCH;
    }
    if (!nonThisReturns(tree, state)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String crvName = qualifyType(state, fix, CRV);
    fix.prefixWith(tree, "@" + crvName + "\n");
    return describeMatch(tree, fix.build());
  }

  private static boolean instanceReturnsBuilder(MethodSymbol sym, VisitorState state) {
    // instance methods
    if (sym.isStatic()) {
      return false;
    }
    // declared in a class with the simple name that contains Builder
    ClassSymbol enclosingClass = sym.owner.enclClass();
    if (!enclosingClass.getSimpleName().toString().endsWith("Builder")) {
      return false;
    }
    // whose return type is the exact type of this
    // or perhaps "a non-Object supertype of the this-type", for interfaces
    Type returnType = sym.getReturnType();
    if (!isSubtype(enclosingClass.asType(), returnType, state)
        || isSameType(returnType, state.getSymtab().objectType, state)) {
      return false;
    }
    return true;
  }

  // TODO(b/236055787): consolidate heuristics for 'return this;'
  boolean nonThisReturns(MethodTree tree, VisitorState state) {

    boolean[] result = {false};
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitReturn(ReturnTree tree, Void unused) {
        if (!returnsThis(tree.getExpression())) {
          result[0] = true;
        }
        return super.visitReturn(tree, null);
      }

      private boolean returnsThis(ExpressionTree tree) {
        return firstNonNull(
            new TreeScanner<Boolean, Void>() {
              @Override
              public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
                return tree.getName().contentEquals("this");
              }

              @Override
              public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                return instanceReturnsBuilder(getSymbol(tree), state);
              }

              @Override
              public Boolean visitConditionalExpression(
                  ConditionalExpressionTree tree, Void unused) {
                return TRUE.equals(tree.getFalseExpression().accept(this, null))
                    && TRUE.equals(tree.getTrueExpression().accept(this, null));
              }

              @Override
              public Boolean visitParenthesized(ParenthesizedTree tree, Void unused) {
                return tree.getExpression().accept(this, null);
              }

              @Override
              public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
                return tree.getExpression().accept(this, null);
              }
            }.scan(tree, null),
            false);
      }
    }.scan(tree.getBody(), null);
    return result[0];
  }
}
