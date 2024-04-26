/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Type;

/**
 * A utility for handling types in cast expressions, shraed by {@link JUnitIncompatibleType} and
 * {@link TruthIncompatibleType}.
 */
final class IgnoringCasts {

  /**
   * Returns the most specific type of a cast or its operand, for example returns {@code byte[]} for
   * both {@code (byte[]) someObject} and {@code (Object) someByteArray}.
   */
  static Type ignoringCasts(Tree tree, VisitorState state) {
    return new SimpleTreeVisitor<Type, Void>() {
      @Override
      protected Type defaultAction(Tree node, Void unused) {
        return getType(node);
      }

      @Override
      public Type visitTypeCast(TypeCastTree node, Void unused) {
        Type castType = getType(node);
        Type expressionType = node.getExpression().accept(this, null);
        return (castType.isPrimitive() || state.getTypes().isSubtype(castType, expressionType))
            ? castType
            : expressionType;
      }

      @Override
      public Type visitParenthesized(ParenthesizedTree node, Void unused) {
        return node.getExpression().accept(this, null);
      }
    }.visit(tree, null);
  }

  private IgnoringCasts() {}
}
