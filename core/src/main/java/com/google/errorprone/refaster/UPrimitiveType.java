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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeKind;

/**
 * {@code UType} representation of primitive {@code Type} instances, specifically including the void
 * type and the null type.
 *
 * @author Louis Wasserman
 */
@AutoValue
abstract class UPrimitiveType extends UType {

  public static UPrimitiveType create(TypeKind typeKind) {
    checkArgument(
        isDeFactoPrimitive(typeKind), "Non-primitive type %s passed to UPrimitiveType", typeKind);
    return new AutoValue_UPrimitiveType(typeKind);
  }

  public abstract TypeKind getKind();

  private static final ImmutableSet<TypeKind> HONORARY_PRIMITIVES =
      ImmutableSet.of(TypeKind.VOID, TypeKind.NULL);

  public static final UPrimitiveType BYTE = create(TypeKind.BYTE);
  public static final UPrimitiveType SHORT = create(TypeKind.SHORT);
  public static final UPrimitiveType INT = create(TypeKind.INT);
  public static final UPrimitiveType LONG = create(TypeKind.LONG);
  public static final UPrimitiveType FLOAT = create(TypeKind.FLOAT);
  public static final UPrimitiveType DOUBLE = create(TypeKind.DOUBLE);
  public static final UPrimitiveType BOOLEAN = create(TypeKind.BOOLEAN);
  public static final UPrimitiveType CHAR = create(TypeKind.CHAR);
  public static final UPrimitiveType NULL = create(TypeKind.NULL);
  public static final UPrimitiveType VOID = create(TypeKind.VOID);

  public static boolean isDeFactoPrimitive(TypeKind kind) {
    return kind.isPrimitive() || HONORARY_PRIMITIVES.contains(kind);
  }

  @Override
  public Choice<Unifier> visitType(Type target, Unifier unifier) {
    return Choice.condition(getKind().equals(target.getKind()), unifier);
  }

  @Override
  public Type inline(Inliner inliner) {
    Symtab symtab = inliner.symtab();
    switch (getKind()) {
      case BYTE:
        return symtab.byteType;
      case SHORT:
        return symtab.shortType;
      case INT:
        return symtab.intType;
      case LONG:
        return symtab.longType;
      case FLOAT:
        return symtab.floatType;
      case DOUBLE:
        return symtab.doubleType;
      case BOOLEAN:
        return symtab.booleanType;
      case CHAR:
        return symtab.charType;
      case VOID:
        return symtab.voidType;
      case NULL:
        return symtab.botType;
      default:
        throw new AssertionError();
    }
  }
}
