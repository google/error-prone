/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.errorprone.dataflow.DataFlow;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/**
 * Provides methods to determine whether an expression is a constant.
 */
public final class Constants {
  private static final ConstantPropagationTransfer TRANSFER = new ConstantPropagationTransfer();

  // TODO(user) I would like to reuse VisitorState.findEnclosing
  private static MethodTree enclosingMethod(TreePath path) {
    while (path != null) {
      Tree node = path.getLeaf();
      if (node instanceof MethodTree) {
        return (MethodTree) node;
      }
      path = path.getParentPath();
    }
    return null;
  }

  public static Number numberValue(Tree tree, TreePath path, Context context) {
    MethodTree method = enclosingMethod(path);
    if (method == null) {
      // TODO(user) this can happen in field initialization.
      // Currently not supported because it only happens in ~2% of cases.
      return null;
    }

    // using full blown constant propagation
    Constant val = DataFlow.dataflow(method, path, context, TRANSFER).getAnalysis().getValue(tree);
    if (val == null || !val.isConstant()) {
      return null;
    }

    return val.getValue();
  }
}
