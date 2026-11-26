/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import org.jspecify.annotations.Nullable;

/// `UMatches` allows conditionally matching a `UExpression` predicated by an error-prone `Matcher`.
@AutoValue
abstract class UMatches extends UExpression {
  public static UMatches create(
      Class<? extends Matcher<? super ExpressionTree>> matcherClass,
      boolean positive,
      UExpression expression) {
    // Verify that we can instantiate the Matcher
    var unused = makeMatcher(matcherClass);

    return new AutoValue_UMatches(positive, matcherClass, expression);
  }

  abstract boolean positive();

  abstract Class<? extends Matcher<? super ExpressionTree>> matcherClass();

  abstract UExpression expression();

  @Override
  protected @Nullable Choice<Unifier> defaultAction(Tree target, @Nullable Unifier unifier) {
    Tree exprTarget = ASTHelpers.stripParentheses(target);
    if (unifier != null && !isPotentiallyAssignableToExpectedType(exprTarget, unifier)) {
      return Choice.none();
    }
    return expression()
        .unify(exprTarget, unifier)
        .filter((Unifier success) -> matches(exprTarget, success) == positive());
  }

  /**
   * Fast conservative check rejecting expressions whose static type cannot possibly satisfy a free
   * variable's declared {@link UClassType}, avoiding expensive {@link Matcher} predicates (such as
   * dataflow analyses) on mismatched types.
   */
  private boolean isPotentiallyAssignableToExpectedType(Tree exprTarget, Unifier unifier) {
    if (!(expression() instanceof UFreeIdent freeIdent)
        || !(exprTarget instanceof JCExpression jcExpr)) {
      return true;
    }
    Type actualType = jcExpr.type;
    if (actualType == null) {
      return true;
    }
    TypeTag tag = actualType.getTag();
    if (tag == TypeTag.BOT || tag == TypeTag.ERROR || tag == TypeTag.NONE) {
      return true;
    }
    UType expectedUType = unifier.expectedArgType(freeIdent.getName().contents());
    if (!(expectedUType instanceof UClassType expectedClassType)) {
      return true;
    }
    ClassSymbol expectedSym =
        unifier.resolveClassOrNull(expectedClassType.fullyQualifiedClass().contents());
    if (expectedSym == null) {
      return false;
    }
    Type erasedExpected = unifier.types().erasure(expectedSym.type);
    if (actualType.isPrimitive()) {
      if (!unifier.autoboxing()) {
        return false;
      }
      actualType = unifier.types().boxedClass(actualType).type;
    } else if (!actualType.isReference()) {
      return true;
    }
    return unifier.types().isAssignable(actualType, erasedExpected);
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    throw new UnsupportedOperationException("@Matches should not appear in an @AfterTemplate");
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return expression().accept(visitor, data);
  }

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  private transient Matcher<? super ExpressionTree> matcher;

  private boolean matches(Tree target, Unifier unifier) {
    if (matcher == null) {
      matcher = makeMatcher(matcherClass());
    }
    return target instanceof ExpressionTree expressionTree
        && matcher.matches(expressionTree, makeVisitorState(target, unifier));
  }

  static <T> T makeMatcher(Class<T> klass) {
    try {
      return klass.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  static VisitorState makeVisitorState(Tree target, Unifier unifier) {
    VisitorState shared = unifier.sharedVisitorState();
    VisitorState state =
        shared != null ? shared : VisitorState.createForUtilityPurposes(unifier.getContext());
    TreePath basePath = unifier.currentPath();
    TreePath path = null;
    if (basePath != null) {
      path = basePath.getLeaf() == target ? basePath : TreePath.getPath(basePath, target);
    }
    if (path == null) {
      path = TreePath.getPath(unifier.compilationUnit(), target);
    }
    if (path != null) {
      state = state.withPath(path);
    }
    return state;
  }
}
