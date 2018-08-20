/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.util;

import com.google.common.collect.Streams;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import java.util.stream.Stream;

/** Annotation-related utilities. */
public final class MoreAnnotations {

  /**
   * Returns declaration annotations of the given symbol, as well as 'top-level' type annotations,
   * including :
   *
   * <ul>
   *   <li>Type annotations of the return type of a method.
   *   <li>Type annotations on the type of a formal parameter or field.
   * </ul>
   *
   * <p>One might expect this to be equivalent to information returned by {@link
   * com.sun.tools.javac.code.Type#getAnnotationMirrors}, but javac doesn't associate type
   * annotation information with types for symbols completed from class files, so that approach
   * doesn't work across compilation boundaries.
   */
  public static Stream<Attribute.Compound> getDeclarationAndTypeAttributes(Symbol sym) {
    Symbol typeAnnotationOwner;
    switch (sym.getKind()) {
      case PARAMETER:
        typeAnnotationOwner = sym.owner;
        break;
      default:
        typeAnnotationOwner = sym;
    }
    return Streams.concat(
        sym.getRawAttributes().stream(),
        typeAnnotationOwner.getRawTypeAttributes().stream()
            .filter(anno -> isAnnotationOnType(sym, anno.position)));
  }

  private static boolean isAnnotationOnType(Symbol sym, TypeAnnotationPosition position) {
    if (!position.location.isEmpty()) {
      return false;
    }
    switch (sym.getKind()) {
      case LOCAL_VARIABLE:
        return position.type == TargetType.LOCAL_VARIABLE;
      case FIELD:
        return position.type == TargetType.FIELD;
      case METHOD:
        return position.type == TargetType.METHOD_RETURN;
      case PARAMETER:
        switch (position.type) {
          case METHOD_FORMAL_PARAMETER:
            return ((MethodSymbol) sym.owner).getParameters().indexOf(sym)
                == position.parameter_index;
          default:
            return false;
        }
      default:
        throw new AssertionError(sym.getKind());
    }
  }

  private MoreAnnotations() {}
}
