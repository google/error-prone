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
import com.google.errorprone.refaster.UTypeVar.TypeWithExpression;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Identifier for a type variable in an AST; this is a syntactic representation of a {@link
 * UTypeVar}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UTypeVarIdent extends UIdent {
  private static final TreeVisitor<Boolean, Void> QUALIFIED_FROM_PACKAGE =
      new SimpleTreeVisitor<Boolean, Void>(false) {

        @Override
        public Boolean visitMemberSelect(MemberSelectTree node, Void p) {
          return node.getExpression().accept(this, null);
        }

        @Override
        public Boolean visitIdentifier(IdentifierTree node, Void p) {
          return ASTHelpers.getSymbol(node) != null
              && ASTHelpers.getSymbol(node).getKind() == ElementKind.PACKAGE;
        }
      };

  public static UTypeVarIdent create(CharSequence name) {
    return new AutoValue_UTypeVarIdent(StringName.of(name));
  }

  @Override
  public abstract StringName getName();

  UTypeVar.Key key() {
    return new UTypeVar.Key(getName());
  }

  @Override
  public JCExpression inline(Inliner inliner) {
    return inliner.getBinding(key()).inline(inliner);
  }

  @Override
  protected Choice<Unifier> defaultAction(Tree target, Unifier unifier) {
    JCExpression expr = (JCExpression) target;
    Type targetType = expr.type;
    if (targetType == null) {
      return Choice.none();
    }
    @Nullable TypeWithExpression boundType = unifier.getBinding(key());
    if (boundType == null) {
      unifier.putBinding(
          key(),
          expr.accept(QUALIFIED_FROM_PACKAGE, null)
              ? TypeWithExpression.create(
                  targetType) /* use the ImportPolicy to refer to this type */
              : TypeWithExpression.create(targetType, expr));
      return Choice.of(unifier);
    } else if (unifier.types().isSameType(targetType, boundType.type())) {
      return Choice.of(unifier);
    }
    return Choice.none();
  }
}
