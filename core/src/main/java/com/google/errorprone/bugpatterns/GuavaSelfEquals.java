/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.sameArgument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

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

/**
 * Points out if an object is tested for equality to itself using Guava Libraries.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(
  name = "GuavaSelfEquals",
  summary = "An object is tested for equality to itself using Guava Libraries",
  explanation =
      "The arguments to this equal method are the same object, so it always returns "
          + "true.  Either change the arguments to point to different objects or substitute true.",
  category = GUAVA,
  severity = ERROR
)
public class GuavaSelfEquals extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to the Guava method Objects.equal() in which the two arguments are the same
   * reference.
   *
   * Example: Objects.equal(foo, foo)
   */
  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> GUAVA_MATCHER =
      allOf(
          staticMethod().onClass("com.google.common.base.Objects").named("equal"),
          sameArgument(0, 1));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!GUAVA_MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    return describe(methodInvocationTree, state);
  }

  private Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    /**
     * Cases:
     * <ol>
     * <li>Objects.equal(foo, foo) ==> Objects.equal(foo, other.foo)</li>
     * <li>Objects.equal(foo, this.foo) ==> Objects.equal(other.foo, this.foo)</li>
     * <li>Objects.equal(this.foo, foo) ==> Objects.equal(this.foo, other.foo)</li>
     * <li>Objects.equal(this.foo, this.foo) ==> Objects.equal(this.foo, other.foo)</li>
     * </ol>
     */

    verifyArgsType(methodInvocationTree);
    List<? extends ExpressionTree> args = methodInvocationTree.getArguments();  
    // Choose argument to replace.
    ExpressionTree toReplace;
    if (args.get(1).getKind() == Kind.IDENTIFIER) {
      toReplace = args.get(1);
    } else if (args.get(0).getKind() == Kind.IDENTIFIER) {
      toReplace = args.get(0);
    } else {
      // If we don't have a good reason to replace one or the other, replace the second.
      toReplace = args.get(1);
    }

    Fix fix = generateFix(methodInvocationTree, state, toReplace);
    return describeMatch(methodInvocationTree, fix);
  }
  
  /** Verifies arguments to be either identifiers or field accesses. */
  protected static void verifyArgsType(MethodInvocationTree methodInvocationTree) {
    // Assumption: Both arguments are either identifiers or field accesses.
    for (ExpressionTree arg : methodInvocationTree.getArguments()) {
      switch (arg.getKind()) {
        case IDENTIFIER:
        case MEMBER_SELECT:
          break;
        default:
          throw new IllegalStateException(
              "Expected arg " + arg + " to be a field access or identifier");
      }
    }
  }

  /** Finds a replacement for toReplace expression tree if possible. */
  protected static Fix generateFix(Tree tree, VisitorState state, ExpressionTree toReplace) {
    Fix fieldFix = fieldFix(toReplace, state);
    if (fieldFix != null) {
      return fieldFix;
    }
    // If we don't find a good field to use, then just replace with "true"
    return SuggestedFix.replace(tree, "true");
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
