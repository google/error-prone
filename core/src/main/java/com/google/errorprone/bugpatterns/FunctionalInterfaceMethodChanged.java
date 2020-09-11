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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_VOID_TYPE;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** @author Louis Wasserman */
@BugPattern(
    name = "FunctionalInterfaceMethodChanged",
    summary =
        "Casting a lambda to this @FunctionalInterface can cause a behavior change from casting to"
            + " a functional superinterface, which is surprising to users.  Prefer decorator"
            + " methods to this surprising behavior.",
    severity = SeverityLevel.ERROR,
    generateExamplesFromTestCases = false)
public class FunctionalInterfaceMethodChanged extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<Tree> IS_FUNCTIONAL_INTERFACE =
      Matchers.symbolHasAnnotation(FunctionalInterface.class);

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    ClassTree enclosingClazz = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (tree.getModifiers().getFlags().contains(Modifier.DEFAULT)
        && IS_FUNCTIONAL_INTERFACE.matches(enclosingClazz, state)) {
      Types types = state.getTypes();
      Set<Symbol> functionalSuperInterfaceSams =
          enclosingClazz.getImplementsClause().stream()
              .filter(t -> IS_FUNCTIONAL_INTERFACE.matches(t, state))
              .map(ASTHelpers::getSymbol)
              .map(TypeSymbol.class::cast)
              .map(types::findDescriptorSymbol) // TypeSymbol to single abstract method of the type
              .collect(toImmutableSet());

      // We designate an override of a superinterface SAM "behavior preserving" if it just
      // calls the SAM of this interface.
      Symbol thisInterfaceSam = types.findDescriptorSymbol(ASTHelpers.getSymbol(enclosingClazz));

      // relatively crude: doesn't verify that the same args are passed in the same order
      // so it can get false positives for behavior-preservingness (false negatives for the check)
      TreeVisitor<Boolean, VisitorState> behaviorPreserving =
          new BehaviorPreservingChecker(thisInterfaceSam);
      if (!Collections.disjoint(
              ASTHelpers.findSuperMethods(ASTHelpers.getSymbol(tree), types),
              functionalSuperInterfaceSams)
          && !tree.accept(behaviorPreserving, state)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }

  private static class BehaviorPreservingChecker extends SimpleTreeVisitor<Boolean, VisitorState> {

    private boolean inBoxedVoidReturningMethod = false;
    private final Symbol methodToCall;

    public BehaviorPreservingChecker(Symbol methodToCall) {
      super(false);
      this.methodToCall = methodToCall;
    }

    @Override
    public Boolean visitMethod(MethodTree node, VisitorState state) {
      boolean prevInBoxedVoidReturningMethod = inBoxedVoidReturningMethod;
      Type returnType = ASTHelpers.getType(node.getReturnType());
      Type boxedVoidType = JAVA_LANG_VOID_TYPE.get(state);
      if (ASTHelpers.isSameType(returnType, boxedVoidType, state)) {
        inBoxedVoidReturningMethod = true;
      }
      boolean result = node.getBody() != null && node.getBody().accept(this, state);
      inBoxedVoidReturningMethod = prevInBoxedVoidReturningMethod;
      return result;
    }

    @Override
    public Boolean visitBlock(BlockTree node, VisitorState state) {
      if (inBoxedVoidReturningMethod) {
        // Must have exactly 2 statements
        if (node.getStatements().size() != 2) {
          return false;
        }

        // Where the first one is a call to the methodToCall
        if (!node.getStatements().get(0).accept(this, state)) {
          return false;
        }

        // And the second one is "return null;"
        if (node.getStatements().get(1) instanceof ReturnTree) {
          ReturnTree returnTree = (ReturnTree) node.getStatements().get(1);
          if (returnTree.getExpression() instanceof LiteralTree) {
            Object returnValue = ((LiteralTree) returnTree.getExpression()).getValue();
            return returnValue == null;
          }
        }
        return false;
      } else {
        return node.getStatements().size() == 1
            && Iterables.getOnlyElement(node.getStatements()).accept(this, state);
      }
    }

    @Override
    public Boolean visitExpressionStatement(ExpressionStatementTree node, VisitorState state) {
      return node.getExpression().accept(this, state);
    }

    @Override
    public Boolean visitReturn(ReturnTree node, VisitorState state) {
      return node.getExpression().accept(this, state);
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree node, VisitorState state) {
      return ASTHelpers.getSymbol(node) == methodToCall;
    }
  }
}
