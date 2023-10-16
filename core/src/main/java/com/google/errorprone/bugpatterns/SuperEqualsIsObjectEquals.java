/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.LIKELY_ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.RETURN;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "`super.equals(obj)` is equivalent to `this == obj` here",
    severity = WARNING,
    tags = LIKELY_ERROR)
public class SuperEqualsIsObjectEquals extends BugChecker implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    var methodSelect = tree.getMethodSelect();
    if (methodSelect.getKind() != MEMBER_SELECT) {
      return NO_MATCH;
    }
    var memberSelect = (MemberSelectTree) methodSelect;
    var expression = memberSelect.getExpression();
    if (expression.getKind() == IDENTIFIER
        && ((IdentifierTree) expression).getName().equals(state.getNames()._super)
        // We can't use a Matcher because onExactClass suffers from b/130658266.
        && enclosingClass(getSymbol(tree)) == state.getSymtab().objectType.tsym
        && memberSelect.getIdentifier().equals(state.getNames().equals)
        /*
         * We ignore an override that is merely `return super.equals(obj)`. Such an override is less
         * likely to be a bug because it may exist for the purpose of adding Javadoc.
         *
         * TODO(cpovirk): Consider flagging even that if the method does *not* have Javadoc.
         */
        && !methodBodyIsOnlyReturnSuperEquals(state)) {
      /*
       * There will often be better fixes than this, some of which would change behavior. But let's
       * at least suggest the simple thing that's always equivalent.
       */
      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree, "this == " + state.getSourceForNode(getOnlyElement(tree.getArguments()))));
    }
    return NO_MATCH;
  }

  private static boolean methodBodyIsOnlyReturnSuperEquals(VisitorState state) {
    var parentPath = state.getPath().getParentPath();
    if (parentPath.getLeaf().getKind() != RETURN) {
      return false;
    }
    var grandparentPath = parentPath.getParentPath();
    var grandparent = grandparentPath.getLeaf();
    if (grandparent.getKind() != BLOCK) {
      return false;
    }
    if (((BlockTree) grandparent).getStatements().size() > 1) {
      return false;
    }
    var greatGrandparent = grandparentPath.getParentPath().getLeaf();
    if (greatGrandparent.getKind() != METHOD) {
      return false;
    }
    return true;
  }
}
