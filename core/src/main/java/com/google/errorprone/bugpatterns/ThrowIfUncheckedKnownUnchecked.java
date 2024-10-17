/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.argumentCount;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import javax.lang.model.type.UnionType;

/** A BugPattern; see the summary. */
@BugPattern(
    summary = "`throwIfUnchecked(knownUnchecked)` is equivalent to `throw knownUnchecked`.",
    severity = WARNING)
public class ThrowIfUncheckedKnownUnchecked extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> IS_THROW_IF_UNCHECKED =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Throwables").named("throwIfUnchecked"),
              staticMethod()
                  .onClass("com.google.common.base.Throwables")
                  .named("propagateIfPossible")),
          argumentCount(1));

  private static final Matcher<ExpressionTree> IS_KNOWN_UNCHECKED =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Type type = ASTHelpers.getType(tree);
          if (type.isUnion()) {
            return ((UnionType) type)
                .getAlternatives().stream().allMatch(t -> isKnownUnchecked(state, (Type) t));
          } else {
            return isKnownUnchecked(state, type);
          }
        }

        boolean isKnownUnchecked(VisitorState state, Type type) {
          Types types = state.getTypes();
          Symtab symtab = state.getSymtab();
          // Check erasure for generics.
          // TODO(cpovirk): Is that necessary here or in ThrowIfUncheckedKnownChecked?
          type = types.erasure(type);
          return types.isSubtype(type, symtab.errorType)
              || types.isSubtype(type, symtab.runtimeExceptionType);
        }
      };

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (IS_THROW_IF_UNCHECKED.matches(tree, state)
        && argument(0, IS_KNOWN_UNCHECKED).matches(tree, state)) {
      var fix = SuggestedFix.builder();
      fix.replace(tree, "throw " + state.getSourceForNode(tree.getArguments().get(0)));
      /*
       * Changing to `throw ...` make the compiler recognize everything afterward in the block as
       * unreachable. To avoid build errors from that, we remove everything afterward.
       *
       * We might still produce build errors if code *after* the block becomes unreachable (because
       * it's now possible to fall out of this block). That seems tolerable.
       */
      var parent = state.getPath().getParentPath().getLeaf();
      var grandparent = state.getPath().getParentPath().getParentPath().getLeaf();
      if (parent.getKind() == EXPRESSION_STATEMENT && grandparent.getKind() == BLOCK) {
        ((BlockTree) grandparent)
            .getStatements().stream().dropWhile(t -> t != parent).skip(1).forEach(fix::delete);
      }
      return describeMatch(tree, fix.build());
    }
    return NO_MATCH;
  }
}
