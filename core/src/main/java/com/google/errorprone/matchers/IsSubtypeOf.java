/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;

/**
 * Support code for isSubtypeOf matchers.
 * @author eaftan@google.com (Eddie Aftandilian
 */
public class IsSubtypeOf {
  public static boolean isSubtypeOf(Tree tree, Type toType, VisitorState state) {
    Types types = state.getTypes();
    return (types.erasure(toType) != null &&
        types.isSubtype(types.erasure(((JCTree) tree).type), types.erasure(toType)));
  }
}
