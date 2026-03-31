/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;

/**
 * Detects when record accessors read uninitialized fields inside a compact constructor. Use the
 * component parameter instead.
 *
 * <p>Example:
 *
 * {@snippet :
 * record R(int x) {
 *   public R {
 *     int y = x(); // BUG: use `x` instead
 *   }
 * }
 * }
 */
@BugPattern(
    summary =
        "Record accessors read uninitialized fields inside a compact constructor. Use the"
            + " component parameter instead.",
    severity = ERROR)
public final class RecordAccessorInCompactConstructor extends BugChecker
    implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodTree enclosingMethod = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (enclosingMethod == null) {
      return Description.NO_MATCH;
    }

    MethodSymbol methodSym = ASTHelpers.getSymbol(enclosingMethod);
    if (methodSym == null || (methodSym.flags() & Flags.COMPACT_RECORD_CONSTRUCTOR) == 0) {
      return Description.NO_MATCH;
    }

    if (state.findEnclosing(LambdaExpressionTree.class) != null) {
      return Description.NO_MATCH;
    }

    MethodSymbol calledMethod = ASTHelpers.getSymbol(tree);
    if (calledMethod == null) {
      return Description.NO_MATCH;
    }

    ClassSymbol recordClass = methodSym.enclClass();
    if (!calledMethod.owner.equals(recordClass)) {
      return Description.NO_MATCH;
    }

    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    if (receiver != null
        && !(receiver instanceof IdentifierTree identifierTree
            && identifierTree.getName().contentEquals("this"))) {
      return Description.NO_MATCH;
    }

    String componentName = null;
    for (RecordComponent rc : recordClass.getRecordComponents()) {
      if (calledMethod.equals(rc.accessor)) {
        componentName = rc.name.toString();
        break;
      }
    }

    if (componentName == null) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            "Record accessors inside compact constructors read uninitialized fields (JLS"
                + " §8.10.4.2). Use the parameter directly.")
        .addFix(SuggestedFix.replace(tree, componentName))
        .build();
  }
}
