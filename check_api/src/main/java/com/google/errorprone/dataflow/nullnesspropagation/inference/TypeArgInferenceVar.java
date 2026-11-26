/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation.inference;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.Tree;

/// TypeArg inference variables correspond to types (and parameters thereof) of AST nodes. The
/// `typeArgSelector` specifies a type (or parameter thereof) of the `astNode` by a series of
/// indices of type parameter lists corresponding to a path into the tree structure of `astNode`'s
/// type.
///
/// For example, if the type of `astNode` is `A<B, C<D, E<F, G>>>`, then inference variables are
/// specified by typeArgSelectors as follows: `A` by the empty list `[]` since it is the base type;
/// `B` by the list `[0]` since it is the 0th type parameter of the base type; `F` by the list
/// `[1,1,0]` since it is the 0th type parameter of the 1st type parameter of the 1st type parameter
/// of the base type; and so on.
///
/// @param typeArgSelector An empty list selects the type of the `astNode` itself, while non-empty
///   lists select type variables within, according to the format described in the class-level
///   Javadoc
record TypeArgInferenceVar(ImmutableList<Integer> typeArgSelector, Tree astNode)
    implements InferenceVariable {
  static TypeArgInferenceVar create(ImmutableList<Integer> typeArgSelector, Tree astNode) {
    return new TypeArgInferenceVar(typeArgSelector, astNode);
  }
}
