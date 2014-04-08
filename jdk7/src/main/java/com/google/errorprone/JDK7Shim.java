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

import com.google.errorprone.fixes.AdjustedPosition7;
import com.google.errorprone.fixes.IndexedPosition7;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import java.util.Map;

public class JDK7Shim implements JDKCompatibleShim {

  @Override
  public DiagnosticPosition getAdjustedPosition(JCTree position, int startPosAdjustment,
      int endPosAdjustment) {
    return new AdjustedPosition7(position, startPosAdjustment, endPosAdjustment);
  }

  @Override
  public DiagnosticPosition getIndexedPosition(int startPos, int endPos) {
    return new IndexedPosition7(startPos, endPos);
  }

  @Override
  public EndPosMap7 getEndPosMap(JCCompilationUnit compilationUnit) {
    if (compilationUnit.endPositions == null) {
      return null;
    }
    return new EndPosMap7(compilationUnit.endPositions);
  }

  @Override
  public void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
    // Nothing required for JDK <= 7.
  }

  @Override
  public int runCompile(Main main, String[] args, Context context, List<JavaFileObject> files,
      Iterable<? extends Processor> processors) {
    return main.compile(args, context, files, processors);
  }

  @Override
  public int getJCTreeTag(JCTree node) {
    return node.getTag();
  }

  @Override
  public Integer getEndPosition(DiagnosticPosition pos, Map<JCTree, Integer> map) {
    return EndPosMap7.getEndPos(pos, map);
  }
}
