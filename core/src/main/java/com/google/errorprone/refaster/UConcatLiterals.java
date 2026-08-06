/*
 * Copyright 2021 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import java.util.ArrayList;
import java.util.List;

/** {@code UExpression} that tries to concatenate literals. */
@AutoValue
public abstract class UConcatLiterals extends UExpression {
  public static UConcatLiterals create(UExpression... expressions) {
    return create(ImmutableList.copyOf(expressions));
  }

  public static AutoValue_UConcatLiterals create(Iterable<? extends UExpression> expressions) {
    return new AutoValue_UConcatLiterals(ImmutableList.copyOf(expressions));
  }

  abstract ImmutableList<UExpression> expressions();

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    TreeMaker treeMaker = inliner.maker();
    return mergeAdjacentLiterals(inliner.inlineList(expressions()), treeMaker).stream()
        .reduce((e1, e2) -> treeMaker.Binary(JCTree.Tag.PLUS, e1, e2))
        .orElseGet(() -> treeMaker.Literal(""));
  }

  private List<JCExpression> mergeAdjacentLiterals(
      List<JCExpression> inlinedExpressions, TreeMaker treeMaker) {
    List<JCExpression> mergedExpressions = new ArrayList<>();

    for (JCExpression expr : inlinedExpressions) {
      concatOrAppendExpression(mergedExpressions, expr, treeMaker);
    }

    return mergedExpressions;
  }

  private static void concatOrAppendExpression(
      List<JCExpression> expressions, JCExpression next, TreeMaker treeMaker) {
    if (expressions.isEmpty() || !(next instanceof LiteralTree)) {
      expressions.add(next);
    } else {
      int prevIndex = expressions.size() - 1;
      JCExpression prev = expressions.get(prevIndex);
      if (!(prev instanceof LiteralTree)
          || prev.getKind() != Kind.STRING_LITERAL && next.getKind() != Kind.STRING_LITERAL) {
        expressions.add(next);
      } else {
        expressions.set(
            prevIndex, treeMaker.Literal(literalAsString(prev) + literalAsString(next)));
      }
    }
  }

  private static String literalAsString(Object expr) {
    return ((LiteralTree) expr).getValue().toString();
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    throw new UnsupportedOperationException("concatLiterals should not appear in an @BeforeTemplate");
  }

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }
}
