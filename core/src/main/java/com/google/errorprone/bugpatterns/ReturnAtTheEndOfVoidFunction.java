/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import javax.lang.model.type.TypeKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "`return;` is unnecessary at the end of void methods and constructors.",
    severity = WARNING)
public final class ReturnAtTheEndOfVoidFunction extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {

    // is a constructor or has a void return type (Void should pass, as `return null;` is required)
    Type returnType = getType(methodTree.getReturnType());
    if (returnType != null && returnType.getKind() != TypeKind.VOID) {
      return NO_MATCH;
    }

    // has a body (not abstract or native)
    BlockTree body = methodTree.getBody();
    if (body == null) {
      return NO_MATCH;
    }

    // body is not empty
    List<? extends StatementTree> statements = body.getStatements();
    if (statements == null || statements.isEmpty()) {
      return NO_MATCH;
    }

    // last statement is a return
    StatementTree lastStatement = Iterables.getLast(statements);
    if (lastStatement.getKind() != StatementTree.Kind.RETURN) {
      return NO_MATCH;
    }

    return describeMatch(methodTree, SuggestedFix.delete(lastStatement));
  }
}
