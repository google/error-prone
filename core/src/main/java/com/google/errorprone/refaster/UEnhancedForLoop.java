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

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.errorprone.util.RuntimeVersion;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;

/**
 * A {@link UTree} representation of a {@link EnhancedForLoopTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UEnhancedForLoop extends USimpleStatement implements EnhancedForLoopTree {
  public static UEnhancedForLoop create(
      UVariableDecl variable, UExpression elements, UStatement statement) {
    return new AutoValue_UEnhancedForLoop(variable, elements, (USimpleStatement) statement);
  }

  @Override
  public abstract UVariableDecl getVariable();

  @Override
  public abstract UExpression getExpression();

  @Override
  public abstract USimpleStatement getStatement();

  // TODO(cushon): support record patterns in enhanced for
  public Tree getVariableOrRecordPattern() {
    return getVariable();
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitEnhancedForLoop(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.ENHANCED_FOR_LOOP;
  }

  @Override
  public JCEnhancedForLoop inline(Inliner inliner) throws CouldNotResolveImportException {
    return makeForeachLoop(
        inliner.maker(),
        getVariable().inline(inliner),
        getExpression().inline(inliner),
        getStatement().inline(inliner));
  }

  private static JCEnhancedForLoop makeForeachLoop(
      TreeMaker maker, JCVariableDecl variable, JCExpression expression, JCStatement statement) {
    try {
      if (RuntimeVersion.isAtLeast20()) {
        return (JCEnhancedForLoop)
            TreeMaker.class
                .getMethod("ForeachLoop", JCTree.class, JCExpression.class, JCStatement.class)
                .invoke(maker, variable, expression, statement);
      } else {
        return (JCEnhancedForLoop)
            TreeMaker.class
                .getMethod(
                    "ForeachLoop", JCVariableDecl.class, JCExpression.class, JCStatement.class)
                .invoke(maker, variable, expression, statement);
      }
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  @Override
  public Choice<Unifier> visitEnhancedForLoop(EnhancedForLoopTree loop, Unifier unifier) {
    return getVariable()
        .unify(loop.getVariable(), unifier)
        .thenChoose(unifications(getExpression(), loop.getExpression()))
        .thenChoose(unifications(getStatement(), loop.getStatement()));
  }
}
