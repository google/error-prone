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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;

import com.google.errorprone.fixes.SuggestedFix;

import com.google.errorprone.matchers.Description;
import com.sun.source.tree.LineMap;

import com.sun.source.tree.CatchTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * "TODO" and "FIXME" should not appear in the error handling
 * logic, because it is often the last line of defense.
 * 
 * For more detail, refer to the paper:
 * "Simple Testing Can Prevent Most Critical Failures: 
 *  An Analysis of Production Failures in Distributed Data-intensive Systems"
 *  Yuan et al. Proceedings of the 11th Symposium on Operating Systems Design 
 *  and Implementation (OSDI), 2014
 *
 * @author yuan@eecg.utoronto.ca (Ding Yuan)
 */
@BugPattern(name = "TodoInCatch",
    summary = "TODO or FIXME in the catch block.",
    explanation = "TODO or FIXME should not appear in the error handling blocks as"
                  + " they are often the last line of defense.",
    category = JDK, maturity = MATURE, severity = ERROR)
public class TodoInCatch extends BugChecker implements CatchTreeMatcher {
  @Override
  public Description matchCatch (CatchTree tree, VisitorState state) {
    LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();
    long startLN = lineMap.getLineNumber(TreeInfo.getStartPos((JCTree) tree));
    long endLN = lineMap.getLineNumber(TreeInfo.endPos((JCTree) (tree.getBlock())));
    String filename = state.getPath().getCompilationUnit().getSourceFile().getName();
    String codeStrs[] = state.getSourceCode().toString().split("\\r?\\n");
    
    boolean foundError = false;
    for (int i = (int) (startLN - 1); i < endLN; i++) {
      // System.out.println("DEBUG: line " + (i+1) + ": " + codeStrs[i]);
      if (codeStrs[i].contains("TODO") || codeStrs[i].contains("FIXME")) {
        foundError = true;
        break;
      }
    }
    
    if (foundError == true) {
      return describeMatch(tree);
    }

    return Description.NO_MATCH;
  }
}
