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
import com.google.common.base.VerifyException;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A {@link UTree} representation of a {@link EnhancedForLoopTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UEnhancedForLoop extends USimpleStatement implements EnhancedForLoopTree {

  private static final long serialVersionUID = 0;

  public static UEnhancedForLoop create(
      UVariableDecl variable, UExpression elements, UStatement statement) {
    // On JDK 20 and above the `EnhancedForLoopTree` interface contains a additional method
    // `getDeclarationKind()`, referencing a  type not available prior to JDK 20. AutoValue
    // generates a corresponding field and accessor for this property. Here we find and invoke the
    // generated constructor with the appropriate arguments, depending on context.
    // See https://github.com/openjdk/jdk20/commit/2cb64a75578ccc15a1dfc8c2843aa11d05ca8aa7.
    // TODO: Simplify this logic once JDK 19 and older are no longer supported.
    return isCompiledWithJdk20Plus()
        ? createJdk20PlusEnhancedForLoop(variable, elements, statement)
        : createPreJdk20EnhancedForLoop(variable, elements, statement);
  }

  private static boolean isCompiledWithJdk20Plus() {
    return Arrays.stream(AutoValue_UEnhancedForLoop.class.getDeclaredMethods())
        .anyMatch(m -> "getDeclarationKind".equals(m.getName()));
  }

  private static UEnhancedForLoop createPreJdk20EnhancedForLoop(
      UVariableDecl variable, UExpression elements, UStatement statement) {
    try {
      return AutoValue_UEnhancedForLoop.class
          .getDeclaredConstructor(UVariableDecl.class, UExpression.class, USimpleStatement.class)
          .newInstance(variable, elements, statement);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private static UEnhancedForLoop createJdk20PlusEnhancedForLoop(
      UVariableDecl variable, UExpression elements, UStatement statement) {
    Object declarationKind = getVariableDeclarationKind();
    try {
      return AutoValue_UEnhancedForLoop.class
          .getDeclaredConstructor(
              declarationKind.getClass(),
              UVariableDecl.class,
              UExpression.class,
              USimpleStatement.class)
          .newInstance(declarationKind, variable, elements, statement);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private static Object getVariableDeclarationKind() {
    Class<?> declarationKind;
    try {
      declarationKind = Class.forName("com.sun.source.tree.EnhancedForLoopTree$DeclarationKind");
    } catch (ClassNotFoundException e) {
      throw new VerifyException("Cannot load `EnhancedForLoopTree.DeclarationKind` enum", e);
    }
    return Arrays.stream(declarationKind.getEnumConstants())
        .filter(v -> "VARIABLE".equals(v.toString()))
        .findFirst()
        .orElseThrow(
            () ->
                new VerifyException(
                    "Enum value `EnhancedForLoopTree.DeclarationKind.VARIABLE` not found"));
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

  private static final Method treeMakerForeachLoopMethod = treeMakerForeachLoopMethod();

  private static Method treeMakerForeachLoopMethod() {
    try {
      return TreeMaker.class.getMethod(
          "ForeachLoop", JCTree.class, JCExpression.class, JCStatement.class);
    } catch (ReflectiveOperationException e1) {
      try {
        return TreeMaker.class.getMethod(
            "ForeachLoop", JCVariableDecl.class, JCExpression.class, JCStatement.class);
      } catch (ReflectiveOperationException e2) {
        e2.addSuppressed(e1);
        throw new LinkageError(e2.getMessage(), e2);
      }
    }
  }

  static JCEnhancedForLoop makeForeachLoop(
      TreeMaker maker, JCVariableDecl variable, JCExpression expression, JCStatement statement) {
    try {
      return (JCEnhancedForLoop)
          treeMakerForeachLoopMethod.invoke(maker, variable, expression, statement);
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
