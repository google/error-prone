/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import java.io.IOException;

/**
 * A representation of a match against a {@code BlockTemplate}. The "location" is the first
 * statement of the match.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
class BlockTemplateMatch extends TemplateMatch {
  private final ImmutableList<JCStatement> statements;

  public BlockTemplateMatch(JCBlock block, Unifier unifier, int start, int end) {
    super(checkNotNull(block).getStatements().get(start), unifier);
    this.statements = ImmutableList.copyOf(block.getStatements().subList(start, end));
  }

  public ImmutableList<JCStatement> getStatements() {
    return statements;
  }

  @Override
  public String getRange(JCCompilationUnit unit) {
    try {
      CharSequence sequence = unit.getSourceFile().getCharContent(true);
      JCTree firstStatement = statements.get(0);
      JCTree lastStatement = statements.get(statements.size() - 1);
      return sequence
          .subSequence(
              firstStatement.getStartPosition(), lastStatement.getEndPosition(unit.endPositions))
          .toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
