/*
 * Copyright 2013 Google Inc. All rights reserved.
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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import javax.annotation.Nullable;

/**
 * A {@link UTree} representation of a local variable declaration.
 *
 * <p>A {@code UVariableDecl} can be unified with any variable declaration which has a matching type
 * and initializer. Annotations and modifiers are preserved for the corresponding replacement, as
 * well as the variable name. {@link ULocalVarIdent} instances are used to represent references to
 * local variables.
 *
 * <p>As a result, we can modify variable declarations and initializations in target code while
 * preserving variable names and other contextual information.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UVariableDecl extends USimpleStatement implements VariableTree {

  public static UVariableDecl create(
      CharSequence identifier, UExpression type, @Nullable UExpression initializer) {
    return new AutoValue_UVariableDecl(StringName.of(identifier), type, initializer);
  }

  public static UVariableDecl create(CharSequence identifier, UExpression type) {
    return create(identifier, type, null);
  }

  @Override
  public abstract StringName getName();

  @Override
  public abstract UExpression getType();

  @Override
  @Nullable
  public abstract UExpression getInitializer();

  ULocalVarIdent.Key key() {
    return new ULocalVarIdent.Key(getName());
  }

  @Override
  public Choice<Unifier> visitVariable(final VariableTree decl, Unifier unifier) {
    return Choice.condition(unifier.getBinding(key()) == null, unifier)
        .thenChoose(unifications(getType(), decl.getType()))
        .thenChoose(unifications(getInitializer(), decl.getInitializer()))
        .transform(
            new Function<Unifier, Unifier>() {
              @Override
              public Unifier apply(Unifier unifier) {
                unifier.putBinding(
                    key(), LocalVarBinding.create(ASTHelpers.getSymbol(decl), decl.getModifiers()));
                return unifier;
              }
            });
  }

  @Override
  public JCVariableDecl inline(Inliner inliner) throws CouldNotResolveImportException {
    return inline(getType(), inliner);
  }

  public JCVariableDecl inlineImplicitType(Inliner inliner) throws CouldNotResolveImportException {
    return inline(null, inliner);
  }

  private JCVariableDecl inline(@Nullable UExpression type, Inliner inliner)
      throws CouldNotResolveImportException {
    Optional<LocalVarBinding> binding = inliner.getOptionalBinding(key());
    JCModifiers modifiers;
    Name name;
    TreeMaker maker = inliner.maker();
    if (binding.isPresent()) {
      modifiers = (JCModifiers) binding.get().getModifiers();
      name = binding.get().getName();
    } else {
      modifiers = maker.Modifiers(0L);
      name = getName().inline(inliner);
    }
    return maker.VarDef(
        modifiers,
        name,
        (type == null) ? null : type.inline(inliner),
        (getInitializer() == null) ? null : getInitializer().inline(inliner));
  }

  @Override
  public Kind getKind() {
    return Kind.VARIABLE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitVariable(this, data);
  }

  @Override
  public ModifiersTree getModifiers() {
    return null;
  }

  @Override
  public ExpressionTree getNameExpression() {
    return null;
  }
}
