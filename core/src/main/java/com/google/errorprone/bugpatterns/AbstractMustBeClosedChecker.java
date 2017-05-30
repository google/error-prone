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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * An abstract check for resources that must be closed; used by {@link FilesLinesLeak} and {@link
 * MustBeClosedChecker}.
 */
public abstract class AbstractMustBeClosedChecker extends BugChecker {

  protected static final Matcher<Tree> HAS_MUST_BE_CLOSED_ANNOTATION =
      symbolHasAnnotation(MustBeClosed.class.getCanonicalName());

  private static final Matcher<ExpressionTree> CLOSE_METHOD =
      instanceMethod().onDescendantOf("java.lang.AutoCloseable").named("close");

  private static final Matcher<Tree> MOCKITO_MATCHER =
      toType(
          MethodInvocationTree.class, staticMethod().onClass("org.mockito.Mockito").named("when"));

  /**
   * Check that constructors and methods annotated with {@link MustBeClosed} occur within the
   * resource variable initializer of a try-with-resources statement.
   */
  protected Description matchNewClassOrMethodInvocation(Tree tree, VisitorState state) {
    Description description = checkClosed(tree, state);
    if (description == NO_MATCH) {
      return NO_MATCH;
    }
    if (AbstractReturnValueIgnored.expectedExceptionTest(tree, state)
        || MOCKITO_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return NO_MATCH;
    }
    return description;
  }

  private Description checkClosed(Tree tree, VisitorState state) {
    MethodTree callerMethodTree = enclosingMethod(state);
    if (state.getPath().getParentPath().getLeaf().getKind() == Tree.Kind.RETURN
        && callerMethodTree != null) {
      // The invocation occurs within a return statement of a method, instead of a lambda
      // expression or anonymous class.

      if (HAS_MUST_BE_CLOSED_ANNOTATION.matches(callerMethodTree, state)) {
        // Ignore invocations of annotated methods and constructors that occur in the return
        // statement of an annotated caller method, since invocations of the caller are enforced.
        return NO_MATCH;
      }

      // The caller method is not annotated, so the closing of the returned resource is not
      // enforced. Suggest fixing this by annotating the caller method.
      return buildDescription(tree)
          .addFix(
              SuggestedFix.builder()
                  .prefixWith(callerMethodTree, "@MustBeClosed\n")
                  .addImport(MustBeClosed.class.getCanonicalName())
                  .build())
          .build();
    }

    if (!inTWR(state)) {
      // The constructor or method invocation does not occur within the resource variable
      // initializer of a try-with-resources statement.
      Description.Builder description = buildDescription(tree);
      addFix(description, tree, state);
      return description.build();
    }
    return NO_MATCH;
  }

  /**
   * Returns the enclosing method of the given visitor state. Returns null if the state is within a
   * lambda expression or anonymous class.
   */
  @Nullable
  private static MethodTree enclosingMethod(VisitorState state) {
    for (Tree node : state.getPath().getParentPath()) {
      switch (node.getKind()) {
        case LAMBDA_EXPRESSION:
        case NEW_CLASS:
          return null;
        case METHOD:
          return (MethodTree) node;
        default:
          break;
      }
    }
    return null;
  }

  /**
   * Returns whether an invocation occurs within the resource variable initializer of a
   * try-with-resources statement.
   */
  // TODO(cushon): This method has been copied from FilesLinesLeak. Move it to a shared class.
  private boolean inTWR(VisitorState state) {
    TreePath path = state.getPath().getParentPath();
    while (path.getLeaf().getKind() == Tree.Kind.CONDITIONAL_EXPRESSION) {
      path = path.getParentPath();
    }
    Symbol sym = getSymbol(path.getLeaf());
    if (!(sym instanceof VarSymbol)) {
      return false;
    }
    VarSymbol var = (VarSymbol) sym;
    return var.getKind() == ElementKind.RESOURCE_VARIABLE || tryFinallyClose(var, path, state);
  }

  private boolean tryFinallyClose(VarSymbol var, TreePath path, VisitorState state) {
    if ((var.flags() & (Flags.FINAL | Flags.EFFECTIVELY_FINAL)) == 0) {
      return false;
    }
    Tree parent = path.getParentPath().getLeaf();
    if (parent.getKind() != Tree.Kind.BLOCK) {
      return false;
    }
    BlockTree block = (BlockTree) parent;
    int idx = block.getStatements().indexOf(path.getLeaf());
    if (idx == -1 || idx == block.getStatements().size() - 1) {
      return false;
    }
    StatementTree next = block.getStatements().get(idx + 1);
    if (!(next instanceof TryTree)) {
      return false;
    }
    TryTree tryTree = (TryTree) next;
    if (tryTree.getFinallyBlock() == null) {
      return false;
    }
    boolean[] closed = {false};
    tryTree
        .getFinallyBlock()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                if (CLOSE_METHOD.matches(tree, state)
                    && Objects.equals(getSymbol(getReceiver(tree)), var)) {
                  closed[0] = true;
                }
                return null;
              }
            },
            null);
    return closed[0];
  }

  protected void addFix(Description.Builder description, Tree tree, VisitorState state) {}
}
