/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_BOOLEAN_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
  name = "EqualsReference",
  summary =
      "== must be used in equals method to check equality to itself"
          + " or an infinite loop will occur.",
  explanation = ".equals() to the same object will result in infinite recursion",
  category = JDK,
  severity = ERROR
)
public class EqualsReference extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> EQUALS_MATCHER =
      allOf(
          methodIsNamed("equals"),
          methodHasParameters(variableType(isSameType("java.lang.Object"))),
          anyOf(methodReturns(BOOLEAN_TYPE), methodReturns(JAVA_LANG_BOOLEAN_TYPE)));

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState visitorState) {
    if (EQUALS_MATCHER.matches(methodTree, visitorState)) {
      VariableTree variableTree = methodTree.getParameters().get(0);
      VarSymbol varSymbol = ASTHelpers.getSymbol(variableTree);
      TreeScannerEquals treeScannerEquals = new TreeScannerEquals(methodTree);
      treeScannerEquals.scan(methodTree.getBody(), varSymbol);
      if (treeScannerEquals.hasIllegalEquals) {
        return describeMatch(methodTree);
      }
    }
    return Description.NO_MATCH;
  }

  private static class TreeScannerEquals extends TreeScanner<Void, VarSymbol> {

    private boolean hasIllegalEquals = false;
    private MethodTree methodTree;

    public TreeScannerEquals(MethodTree currMethodTree) {
      methodTree = currMethodTree;
    }

    @Override
    public Void visitMethodInvocation(
        MethodInvocationTree methodInvocationTree, VarSymbol varSymbol) {
      ExpressionTree methodSelectTree = methodInvocationTree.getMethodSelect();
      MemberSelectTree memberSelectTree;
      boolean hasParameterAndSameSymbol =
          methodInvocationTree.getArguments().size() == 1
              && Objects.equals(
                  ASTHelpers.getSymbol(methodInvocationTree.getArguments().get(0)), varSymbol);
      if (methodSelectTree instanceof MemberSelectTree) {
        memberSelectTree = (MemberSelectTree) methodSelectTree;
        // this.equals(o)
        // not using o.equals(this) because all instances of this were false positives
        // (people checked to see if o was an instance of this class)
        if (memberSelectTree.getExpression().toString().equals("this")
            && Objects.equals(
                ASTHelpers.getSymbol(methodTree), ASTHelpers.getSymbol(memberSelectTree))
            && hasParameterAndSameSymbol) {
          hasIllegalEquals = true;
        }
      } else if (methodInvocationTree.getMethodSelect() instanceof IdentifierTree) {
        IdentifierTree methodSelect = (IdentifierTree) methodInvocationTree.getMethodSelect();
        if (Objects.equals(ASTHelpers.getSymbol(methodTree), ASTHelpers.getSymbol(methodSelect))
            && hasParameterAndSameSymbol) {
          hasIllegalEquals = true;
        }
      }
      return super.visitMethodInvocation(methodInvocationTree, varSymbol);
    }
  }
}
