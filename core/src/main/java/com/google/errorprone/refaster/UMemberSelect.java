/*
 * Copyright 2014 Google Inc. All rights reserved.
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
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;

/**
 * {@link UTree} version of {@link MemberSelectTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UMemberSelect extends UExpression implements MemberSelectTree {
  /**
   * Use of this string as an expression in a member select will cause this method select to be
   * inlined as an identifier. I.e., "".foo will be inlined as foo.
   */
  public static final String CONVERT_TO_IDENT = "";

  public static UMemberSelect create(UExpression expression, CharSequence identifier, UType type) {
    return new AutoValue_UMemberSelect(expression, StringName.of(identifier), type);
  }

  @Override
  public abstract UExpression getExpression();

  @Override
  public abstract StringName getIdentifier();

  abstract UType type();

  @Override
  public Choice<Unifier> visitMemberSelect(MemberSelectTree fieldAccess, Unifier unifier) {
    if (ASTHelpers.getSymbol(fieldAccess) != null) {
      return getIdentifier()
          .unify(fieldAccess.getIdentifier(), unifier)
          .thenChoose(unifications(getExpression(), fieldAccess.getExpression()))
          .thenChoose(unifications(type(), ASTHelpers.getSymbol(fieldAccess).asType()));
    }
    return Choice.none();
  }

  @Override
  public Choice<Unifier> visitIdentifier(final IdentifierTree ident, Unifier unifier) {
    Symbol sym = ASTHelpers.getSymbol(ident);
    if (sym != null && sym.owner.type != null) {
      JCExpression thisIdent = unifier.thisExpression(sym.owner.type);
      return getIdentifier()
          .unify(ident.getName(), unifier)
          .thenChoose(unifications(getExpression(), thisIdent))
          .thenChoose(unifications(type(), sym.asType()));
    }
    return Choice.none();
  }

  @Override
  public Kind getKind() {
    return Kind.MEMBER_SELECT;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitMemberSelect(this, data);
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    JCExpression expression = getExpression().inline(inliner);
    if (expression.toString().equals(CONVERT_TO_IDENT)) {
      return inliner.maker().Ident(getIdentifier().inline(inliner));
    }
    // TODO(lowasser): consider inlining this.foo() as foo()
    return inliner.maker().Select(getExpression().inline(inliner), getIdentifier().inline(inliner));
  }
}
