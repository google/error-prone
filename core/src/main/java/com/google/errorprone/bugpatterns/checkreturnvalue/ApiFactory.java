/*
 * Copyright 2022 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.StructuralTypeMapping;
import com.sun.tools.javac.code.TypeMetadata;
import com.sun.tools.javac.code.Types;

/** Utility method to produce {@link Api} objects from javac {@link MethodSymbol}. */
public final class ApiFactory {

  /** Returns the {@code Api} representation of the given {@code symbol}. */
  public static Api fromSymbol(MethodSymbol symbol, Types types) {
    return Api.internalCreate(
        symbol.owner.getQualifiedName().toString(),
        symbol.name.toString(),
        symbol.getParameters().stream()
            .map(p -> fullyErasedAndUnannotatedType(p.type, types))
            .collect(toImmutableList()));
  }

  static String fullyErasedAndUnannotatedType(Type type, Types types) {
    // Removes type arguments, replacing w/ upper bounds
    Type erasedType = types.erasureRecursive(type);
    Type unannotatedType = erasedType.accept(ANNOTATION_REMOVER, null);
    return unannotatedType.toString();
  }

  /**
   * Removes type metadata (e.g.: type annotations) from types, as well as from "containing
   * structures" like arrays. Notably, this annotation remover doesn't handle Type parameters, as it
   * only attempts to handle erased types.
   */
  private static final StructuralTypeMapping<Void> ANNOTATION_REMOVER =
      new StructuralTypeMapping<>() {
        @Override
        public Type visitType(Type t, Void unused) {
          return t.baseType();
        }

        @Override
        public Type visitClassType(Type.ClassType t, Void unused) {
          return super.visitClassType(t.cloneWithMetadata(TypeMetadata.EMPTY), unused);
        }

        // Remove annotations from all enclosing containers
        @Override
        public Type visitArrayType(Type.ArrayType t, Void unused) {
          return super.visitArrayType(t.cloneWithMetadata(TypeMetadata.EMPTY), unused);
        }
      };

  private ApiFactory() {}
}
