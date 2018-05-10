/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;

/**
 * {@link BugChecker} adds a null check to equals() method implementations which don't satisfy the
 * null contract of equals() method i.e. Object.equals(null) should return false.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "EqualsBrokenForNull",
    summary = "equals() implementation throws NullPointerException when given null",
    severity = SeverityLevel.WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class EqualsBrokenForNull extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> MATCHER =
      allOf(
          methodIsNamed("equals"),
          methodHasParameters(variableType(isSameType("java.lang.Object"))),
          methodReturns(BOOLEAN_TYPE));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    VarSymbol varSymbol = ASTHelpers.getSymbol(getOnlyElement(tree.getParameters()));
    NullnessAnalysis analysis = NullnessAnalysis.instance(state.context);
    // we run nullness analysis on all the subtrees and match if there is a method invocation on
    // the argument to the equals method.
    boolean[] crashesWithNull = {false};
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree node, Void aVoid) {
        if (!crashesWithNull[0]) {
          if (Objects.equals(varSymbol, ASTHelpers.getSymbol(node.getExpression()))) {
            Nullness nullness =
                analysis.getNullness(
                    new TreePath(getCurrentPath(), node.getExpression()), state.context);
            if (nullness == Nullness.NULLABLE) {
              crashesWithNull[0] = true;
            }
          }
        }
        return super.visitMemberSelect(node, aVoid);
      }
    }.scan(state.getPath(), null);
    if (!crashesWithNull[0]) {
      return NO_MATCH;
    }
    String stringAddition =
        String.format("if (%s == null) { return false; }\n", varSymbol.name.toString());
    Fix fix = SuggestedFix.prefixWith(tree.getBody().getStatements().get(0), stringAddition);
    return describeMatch(tree, fix);
  }
}
