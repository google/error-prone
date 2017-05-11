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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterables;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.Names;
import javax.annotation.Nullable;

/**
 * Free identifier that can be bound to any expression of the appropriate type.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UFreeIdent extends UIdent {
  static class Key extends Bindings.Key<JCExpression> {
    Key(CharSequence name) {
      super(name.toString());
    }
  }

  public static UFreeIdent create(CharSequence identifier) {
    return new AutoValue_UFreeIdent(StringName.of(identifier));
  }

  @Override
  public abstract StringName getName();

  public Key key() {
    return new Key(getName());
  }

  @Override
  public JCExpression inline(Inliner inliner) {
    return inliner.getBinding(key());
  }

  private static boolean trueOrNull(@Nullable Boolean condition) {
    return condition == null || condition;
  }

  @Override
  public Choice<Unifier> visitIdentifier(IdentifierTree node, Unifier unifier) {
    Names names = Names.instance(unifier.getContext());
    return node.getName().equals(names._super)
        ? Choice.<Unifier>none()
        : defaultAction(node, unifier);
  }

  @Override
  protected Choice<Unifier> defaultAction(Tree target, final Unifier unifier) {
    if (target instanceof JCExpression) {
      JCExpression expression = (JCExpression) target;

      JCExpression currentBinding = unifier.getBinding(key());

      // Check that the expression does not reference any template-local variables.
      boolean isGood =
          trueOrNull(
              new TreeScanner<Boolean, Void>() {
                @Override
                public Boolean reduce(@Nullable Boolean left, @Nullable Boolean right) {
                  return trueOrNull(left) && trueOrNull(right);
                }

                @Override
                public Boolean visitIdentifier(IdentifierTree ident, Void v) {
                  Symbol identSym = ASTHelpers.getSymbol(ident);
                  for (ULocalVarIdent.Key key :
                      Iterables.filter(unifier.getBindings().keySet(), ULocalVarIdent.Key.class)) {
                    if (identSym == unifier.getBinding(key).getSymbol()) {
                      return false;
                    }
                  }
                  return true;
                }
              }.scan(expression, null));
      if (!isGood) {
        return Choice.none();
      } else if (currentBinding == null) {
        unifier.putBinding(key(), expression);
        return Choice.of(unifier);
      } else if (currentBinding.toString().equals(expression.toString())) {
        // TODO(lowasser): try checking types here in a way that doesn't reject
        // different wildcard captures
        // If it's the same code, treat it as the same expression.
        return Choice.of(unifier);
      }
    }
    return Choice.none();
  }
}
