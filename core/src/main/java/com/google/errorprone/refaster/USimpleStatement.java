/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import com.google.common.base.Function;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;

/**
 * A {@code UStatement} that matches exactly one statement, and inlines to exactly one statement.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
abstract class USimpleStatement extends UTree<JCStatement> implements UStatement {

  @Override
  public List<JCStatement> inlineStatements(Inliner inliner) throws CouldNotResolveImportException {
    return List.of(inline(inliner));
  }

  private static Function<Unifier, UnifierWithUnconsumedStatements> withUnconsumed(
      final java.util.List<? extends StatementTree> statements) {
    return (Unifier unifier) -> UnifierWithUnconsumedStatements.create(unifier, statements);
  }

  @Override
  public Choice<UnifierWithUnconsumedStatements> apply(UnifierWithUnconsumedStatements state) {
    java.util.List<? extends StatementTree> unconsumedStatements = state.unconsumedStatements();
    if (unconsumedStatements.isEmpty()) {
      return Choice.none();
    }
    java.util.List<? extends StatementTree> remainingStatements =
        unconsumedStatements.subList(1, unconsumedStatements.size());
    return unify(unconsumedStatements.get(0), state.unifier())
        .transform(withUnconsumed(remainingStatements));
  }
}
