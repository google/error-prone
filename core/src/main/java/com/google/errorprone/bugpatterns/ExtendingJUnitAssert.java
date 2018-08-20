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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;

/** @author kayco@google.com (Kayla Walker) */
@BugPattern(
    name = "ExtendingJUnitAssert",
    summary =
        "When only using JUnit Assert's static methods, "
            + "you should import statically instead of extending.",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ExtendingJUnitAssert extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ExpressionTree> STATIC_ASSERT =
      staticMethod().onClass("org.junit.Assert").withAnyName();

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    Tree extendsClause = tree.getExtendsClause();
    Type type = ASTHelpers.getType(extendsClause);
    if (ASTHelpers.isSameType(type, state.getTypeFromString("org.junit.Assert"), state)) {
      return describeMatch(extendsClause, fixAsserts(tree, state));
    }
    return Description.NO_MATCH;
  }

  private SuggestedFix fixAsserts(ClassTree tree, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            if (STATIC_ASSERT.matches(tree, state)) {
              String assertType = ASTHelpers.getSymbol(tree).getSimpleName().toString();
              fix.addStaticImport("org.junit.Assert." + assertType);
            }
            return super.visitMethodInvocation(tree, unused);
          }
        },
        null);

    Tree extendsClause = tree.getExtendsClause();
    int startOfClass = ((JCTree) tree).getStartPosition();
    int endOfExtendsClause = state.getEndPosition(extendsClause);
    int extendsPosInClass = endOfExtendsClause - startOfClass;

    List<ErrorProneToken> tokens = state.getTokensForNode(tree);

    int max = 0;
    for (ErrorProneToken token : tokens) {
      if (token.pos() > extendsPosInClass) {
        break;
      }
      if (token.kind() == TokenKind.EXTENDS) {
        int curr = token.pos();
        if (curr > max) {
          max = curr;
        }
      }
    }
    int startPos = ((JCTree) tree).getStartPosition() + max;
    return fix.replace(startPos, endOfExtendsClause, "").build();
  }
}
