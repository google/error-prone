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

package com.google.errorprone.dataflow;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/** An interface to the constant propagation analysis. */
public final class ConstantPropagationAnalysis {

  private static final ConstantPropagationTransfer CONSTANT_PROPAGATION =
      new ConstantPropagationTransfer();

  /**
   * Returns the value of the leaf of {@code exprPath}, if it is determined to be a constant (always
   * evaluates to the same numeric value), and null otherwise. Note that returning null does not
   * necessarily mean the expression is *not* a constant.
   */
  public static Number numberValue(TreePath exprPath, Context context) {
    Constant val = DataFlow.expressionDataflow(exprPath, context, CONSTANT_PROPAGATION);
    if (val == null || !val.isConstant()) {
      return null;
    }
    return val.getValue();
  }
}
