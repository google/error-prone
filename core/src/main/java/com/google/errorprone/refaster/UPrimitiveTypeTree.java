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
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import javax.lang.model.type.TypeKind;

/**
 * {@link UTree} version of {@link UPrimitiveTypeTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UPrimitiveTypeTree extends UExpression implements PrimitiveTypeTree {

  public static UPrimitiveTypeTree create(TypeTag tag) {
    return new AutoValue_UPrimitiveTypeTree(tag);
  }

  abstract TypeTag typeTag();

  public static final UPrimitiveTypeTree BYTE = create(TypeTag.BYTE);
  public static final UPrimitiveTypeTree SHORT = create(TypeTag.SHORT);
  public static final UPrimitiveTypeTree INT = create(TypeTag.INT);
  public static final UPrimitiveTypeTree LONG = create(TypeTag.LONG);
  public static final UPrimitiveTypeTree FLOAT = create(TypeTag.FLOAT);
  public static final UPrimitiveTypeTree DOUBLE = create(TypeTag.DOUBLE);
  public static final UPrimitiveTypeTree BOOLEAN = create(TypeTag.BOOLEAN);
  public static final UPrimitiveTypeTree CHAR = create(TypeTag.CHAR);
  public static final UPrimitiveTypeTree NULL = create(TypeTag.BOT);
  public static final UPrimitiveTypeTree VOID = create(TypeTag.VOID);

  @Override
  public Choice<Unifier> visitPrimitiveType(PrimitiveTypeTree tree, Unifier unifier) {
    return Choice.condition(getPrimitiveTypeKind().equals(tree.getPrimitiveTypeKind()), unifier);
  }

  @Override
  public Kind getKind() {
    return Kind.PRIMITIVE_TYPE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitPrimitiveType(this, data);
  }

  @Override
  public TypeKind getPrimitiveTypeKind() {
    return typeTag().getPrimitiveTypeKind();
  }

  @Override
  public JCExpression inline(Inliner inliner) {
    return inliner.maker().TypeIdent(typeTag());
  }
}
