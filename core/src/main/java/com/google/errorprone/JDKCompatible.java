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

package com.google.errorprone;

import com.google.errorprone.dataflow.DataFlow;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransfer;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessValue;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.AbstractLog;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;

import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.tools.JavaFileObject;

/**
 * An abstraction over JDK version-specific APIs.
 */
public final class JDKCompatible {

  /** ErrorProneEndPosMap factory. */
  public static ErrorProneEndPosMap getEndPosMap(JCCompilationUnit compilationUnit) {
    return EndPosMap8.fromCompilationUnit(compilationUnit);
  }

  /**
   * In JDK 8, the EndPosTable is guarded against being set twice. Use reflection to unset it
   * so re-parsing for end positions works.
   *
   * TODO(user): kill this with fire if -Xjcov ever gets turned on by default
   * (https://code.google.com/p/error-prone/issues/detail?id=228)
   */
  private static final Method ABSTRACT_LOG__GET_SOURCE;
  private static final Field DIAGNOSTIC_SOURCE__END_POS_TABLE;
  static {
    try {
      ABSTRACT_LOG__GET_SOURCE =
          AbstractLog.class.getDeclaredMethod("getSource", JavaFileObject.class);
      ABSTRACT_LOG__GET_SOURCE.setAccessible(true);

      DIAGNOSTIC_SOURCE__END_POS_TABLE =
          DiagnosticSource.class.getDeclaredField("endPosTable");
      DIAGNOSTIC_SOURCE__END_POS_TABLE.setAccessible(true);
    } catch (Exception e) {
      throw new LinkageError(e.getMessage());
    }
  }
  public static void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
    try {
      DiagnosticSource diagnosticSource = (DiagnosticSource)
          ABSTRACT_LOG__GET_SOURCE.invoke(compiler.log, sourceFile);
      DIAGNOSTIC_SOURCE__END_POS_TABLE.set(diagnosticSource, null);
    } catch (Exception e) {
      throw new LinkageError(e.getMessage());
    }
  }

  private static final ConstantPropagationTransfer CONSTANT_PROPAGATION =
      new ConstantPropagationTransfer();
  private static final NullnessPropagationTransfer NULLNESS_PROPAGATION =
      new NullnessPropagationTransfer();

  /**
   * Returns the value of the leaf of {@code exprPath}, if it is determined to be a constant
   * (always evaluates to the same numeric value), and null otherwise.
   * Note that returning null does not necessarily mean the expression is *not* a constant.
   */
  public static Number numberValue(TreePath exprPath, Context context) {
    Constant val = DataFlow.expressionDataflow(exprPath, context, CONSTANT_PROPAGATION);
    if (val == null || !val.isConstant()) {
      return null;
    }
    return val.getValue();
  }

  /**
   * Returns true if the leaf of {@code exprPath} is non-null.
   * Note that returning false does not necessarily mean that the expression can be null.
   */
  public static boolean isDefinitelyNonNull(TreePath exprPath, Context context) {
    NullnessValue val = DataFlow.expressionDataflow(exprPath, context, NULLNESS_PROPAGATION);
    return val != null && val == NullnessValue.NONNULL;
  }

  /**
   * Returns true if the leaf of {@code exprPath} is null.
   * Note that returning false does not necessarily mean that the expression can be non-null.
   */
  public static boolean isDefinitelyNull(TreePath exprPath, Context context) {
    NullnessValue val = DataFlow.expressionDataflow(exprPath, context, NULLNESS_PROPAGATION);
    return val != null && val == NullnessValue.NULL;
  }
}
