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

import com.google.errorprone.fixes.AdjustedPosition6;
import com.google.errorprone.fixes.IndexedPosition6;

import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.parser.Lexer;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Name;

import java.util.Collections;
import java.util.Map;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

/** A JDK6 compatible {@link JDKCompatibleShim} */
public class JDK6Shim implements JDKCompatibleShim {

  @Override
  public DiagnosticPosition getAdjustedPosition(JCTree position, int startPosAdjustment,
      int endPosAdjustment) {
    return new AdjustedPosition6(position, startPosAdjustment, endPosAdjustment);
  }

  @Override
  public DiagnosticPosition getIndexedPosition(int startPos, int endPos) {
    return new IndexedPosition6(startPos, endPos);
  }

  @Override
  public EndPosMap6 getEndPosMap(JCCompilationUnit compilationUnit) {
    if (compilationUnit.endPositions == null) {
      return null;
    }
    return new EndPosMap6(compilationUnit.endPositions);
  }

  @Override
  public void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
    // Nothing required for JDK <= 7.
  }

  @Override
  public int runCompile(
      Main main,
      String[] args,
      Context context,
      com.sun.tools.javac.util.List<JavaFileObject> files,
      Iterable<? extends Processor> processors) {
    return main.compile(args, context, files, processors);
  }

  @Override
  public int getJCTreeTag(JCTree node) {
    return node.getTag();
  }

  @Override
  public Integer getEndPosition(DiagnosticPosition pos, Map<JCTree, Integer> map) {
    return EndPosMap6.getEndPos(pos, map);
  }

  @Override
  public java.util.List<? extends Tree> getTryTreeResources(TryTree tree) {
    return Collections.<JCTree>emptyList();
  }

  @Override
  public Name lookupName(Context context, String name) {
    return Name.Table.instance(context).fromString(name);
  }

  @Override
  public JCExpression parseString(String string, Context context) {
    Lexer lexer = Scanner.Factory.instance(context).newScanner(string);
    Parser parser = Parser.Factory.instance(context).newParser(lexer, true, true);
    JCExpression result = parser.expression();
    int len = (parser.getEndPos(result) - result.getStartPosition());
    if (len != string.length()) {
      throw new IllegalArgumentException("Didn't parse entire string.");
    }
    return result;
  }

  @Override
  public Number numberValue(Tree tree, TreePath path, Context context) {
    return null;
  }
}
