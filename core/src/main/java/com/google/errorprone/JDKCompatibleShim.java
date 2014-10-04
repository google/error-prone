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

import com.sun.source.util.TreePath;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Map;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

/**
 * See com.google.errorprone.JDKCompatible.
 */
interface JDKCompatibleShim {
  DiagnosticPosition getAdjustedPosition(
      JCTree position,
      int startPosAdjustment,
      int endPosAdjustment);
  DiagnosticPosition getIndexedPosition(int startPos, int endPos);
  ErrorProneEndPosMap getEndPosMap(JCCompilationUnit compilationUnit);
  void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile);
  int runCompile(
      Main main,
      String[] args,
      Context context,
      com.sun.tools.javac.util.List<JavaFileObject> files,
      Iterable<? extends Processor> processors);
  int getJCTreeTag(JCTree node);
  Integer getEndPosition(DiagnosticPosition pos, Map<JCTree, Integer> map);
  JCExpression parseString(String string, Context context);
  Number numberValue(TreePath exprPath, Context context);
  boolean isDefinitelyNonNull(TreePath exprPath, Context context);
  boolean isDefinitelyNull(TreePath exprPath, Context context);
}
