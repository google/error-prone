/*
 * Copyright 2015 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.sun.tools.javac.code.Flags.EFFECTIVELY_FINAL;
import static com.sun.tools.javac.code.Flags.FINAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/**
 * Enforce that @CompileTimeConstant parameters are final or effectively final.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    name = "NonFinalCompileTimeConstant",
    summary = "@CompileTimeConstant parameters should be final or effectively final",
    category = JDK,
    severity = ERROR)
public class NonFinalCompileTimeConstant extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getBody() == null) {
      return NO_MATCH;
    }
    for (VariableTree parameter : tree.getParameters()) {
      VarSymbol sym = ASTHelpers.getSymbol(parameter);
      if (sym == null) {
        continue;
      }
      if (!CompileTimeConstantExpressionMatcher.hasCompileTimeConstantAnnotation(state, sym)) {
        continue;
      }
      if ((sym.flags() & FINAL) == FINAL
          || (sym.flags() & EFFECTIVELY_FINAL) == EFFECTIVELY_FINAL) {
        continue;
      }
      return describeMatch(parameter);
    }
    return NO_MATCH;
  }
}
