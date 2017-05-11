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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableBiMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link LiteralTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class ULiteral extends UExpression implements LiteralTree {

  public static ULiteral nullLit() {
    return create(Kind.NULL_LITERAL, null);
  }

  public static ULiteral intLit(int value) {
    return create(Kind.INT_LITERAL, value);
  }

  public static ULiteral longLit(long value) {
    return create(Kind.LONG_LITERAL, value);
  }

  public static ULiteral floatLit(float value) {
    return create(Kind.FLOAT_LITERAL, value);
  }

  public static ULiteral doubleLit(double value) {
    return create(Kind.DOUBLE_LITERAL, value);
  }

  public static ULiteral booleanLit(boolean value) {
    return create(Kind.BOOLEAN_LITERAL, value);
  }

  public static ULiteral charLit(char value) {
    return create(Kind.CHAR_LITERAL, value);
  }

  public static ULiteral stringLit(String value) {
    return create(Kind.STRING_LITERAL, value);
  }

  private static final ImmutableBiMap<Kind, TypeTag> LIT_KIND_TAG =
      new ImmutableBiMap.Builder<Kind, TypeTag>()
          .put(Kind.INT_LITERAL, TypeTag.INT)
          .put(Kind.LONG_LITERAL, TypeTag.LONG)
          .put(Kind.FLOAT_LITERAL, TypeTag.FLOAT)
          .put(Kind.DOUBLE_LITERAL, TypeTag.DOUBLE)
          .put(Kind.CHAR_LITERAL, TypeTag.CHAR)
          .put(Kind.BOOLEAN_LITERAL, TypeTag.BOOLEAN)
          .put(Kind.NULL_LITERAL, TypeTag.BOT)
          .put(Kind.STRING_LITERAL, TypeTag.CLASS)
          .build();

  public static ULiteral create(Kind kind, Object value) {
    checkArgument(LIT_KIND_TAG.containsKey(kind), "%s is not a literal kind", kind);
    return new AutoValue_ULiteral(kind, value);
  }

  @Override
  public abstract Kind getKind();

  @Override
  @Nullable
  public abstract Object getValue();

  private static boolean integral(@Nullable Object o) {
    return o instanceof Integer || o instanceof Long;
  }

  private static boolean match(@Nullable Object a, @Nullable Object b) {
    if (a instanceof Number && b instanceof Number) {
      return (integral(a) && integral(b))
          ? ((Number) a).longValue() == ((Number) b).longValue()
          : Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue()) == 0;
    } else {
      return Objects.equals(a, b);
    }
  }

  @Override
  public Choice<Unifier> visitLiteral(LiteralTree literal, Unifier unifier) {
    return Choice.condition(match(getValue(), literal.getValue()), unifier);
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitLiteral(this, data);
  }

  @Override
  public JCLiteral inline(Inliner inliner) {
    Object value = this.getValue();
    switch (getKind()) {
      case CHAR_LITERAL:
        // Why do they do it like this?  I wish I knew.
        value = (int) ((Character) value).charValue();
        break;
      case BOOLEAN_LITERAL:
        value = ((Boolean) value) ? 1 : 0;
        break;
      default:
        // do nothing
    }
    return inliner.maker().Literal(LIT_KIND_TAG.get(getKind()), value);
  }

  @Override
  public UExpression negate() {
    checkState(getKind() == Kind.BOOLEAN_LITERAL, "Cannot negate a non-Boolean literal");
    return booleanLit(!((Boolean) getValue()));
  }
}
