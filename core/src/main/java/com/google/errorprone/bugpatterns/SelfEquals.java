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
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.receiverSameAsArgument;
import static com.google.errorprone.matchers.Matchers.sameArgument;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.List;
import javax.annotation.Nullable;

/** @author bhagwani@google.com (Sumit Bhagwani) */
@BugPattern(
    name = "SelfEquals",
    summary = "Testing an object for equality with itself will always be true.",
    severity = ERROR)
public class SelfEquals extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<Tree> ASSERTION =
      toType(
          MethodInvocationTree.class,
          staticMethod().anyClass().namedAnyOf("assertTrue", "assertThat"));

  private static final Matcher<MethodInvocationTree> INSTANCE_MATCHER =
      allOf(instanceEqualsInvocation(), receiverSameAsArgument(0));

  private static final Matcher<MethodInvocationTree> STATIC_MATCHER =
      allOf(staticEqualsInvocation(), sameArgument(0, 1));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ASSERTION.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> args = tree.getArguments();
    ExpressionTree toReplace;
    if (INSTANCE_MATCHER.matches(tree, state)) {
      toReplace = args.get(0);
    } else if (STATIC_MATCHER.matches(tree, state)) {
      if (args.get(0).getKind() == Kind.IDENTIFIER && args.get(1).getKind() != Kind.IDENTIFIER) {
        toReplace = args.get(0);
      } else {
        toReplace = args.get(1);
      }
    } else {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    Fix fix = fieldFix(toReplace, state);
    if (fix != null) {
      description.addFix(fix);
    }
    return description.build();
  }

  @Nullable
  protected static Fix fieldFix(Tree toReplace, VisitorState state) {
    TreePath path = state.getPath();
    while (path != null
        && path.getLeaf().getKind() != Kind.CLASS
        && path.getLeaf().getKind() != Kind.BLOCK) {
      path = path.getParentPath();
    }
    if (path == null) {
      return null;
    }
    List<? extends JCTree> members;
    // Must be block or class
    if (path.getLeaf().getKind() == Kind.CLASS) {
      members = ((JCClassDecl) path.getLeaf()).getMembers();
    } else {
      members = ((JCBlock) path.getLeaf()).getStatements();
    }
    for (JCTree jcTree : members) {
      if (jcTree.getKind() == Kind.VARIABLE) {
        JCVariableDecl declaration = (JCVariableDecl) jcTree;
        TypeSymbol variableTypeSymbol =
            state.getTypes().erasure(ASTHelpers.getType(declaration)).tsym;

        if (ASTHelpers.getSymbol(toReplace).isMemberOf(variableTypeSymbol, state.getTypes())) {
          if (toReplace.getKind() == Kind.IDENTIFIER) {
            return SuggestedFix.prefixWith(toReplace, declaration.getName() + ".");
          } else {
            return SuggestedFix.replace(
                ((JCFieldAccess) toReplace).getExpression(), declaration.getName().toString());
          }
        }
      }
    }
    return null;
  }
}
