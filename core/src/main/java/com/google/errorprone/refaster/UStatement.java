/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.errorprone.refaster.UStatement.UnifierWithUnconsumedStatements;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import java.io.Serializable;
import java.util.List;

/**
 * {@link UTree} representation of a {@link StatementTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public interface UStatement
    extends Serializable,
        StatementTree,
        Function<UnifierWithUnconsumedStatements, Choice<UnifierWithUnconsumedStatements>> {
  /** Tuple of a Unifier and a list of statements that are still waiting to be matched. */
  @AutoValue
  public abstract static class UnifierWithUnconsumedStatements {
    public static UnifierWithUnconsumedStatements create(
        Unifier unifier, List<? extends StatementTree> unconsumedStatements) {
      return new AutoValue_UStatement_UnifierWithUnconsumedStatements(
          unifier, unconsumedStatements);
    }

    public abstract Unifier unifier();

    public abstract List<? extends StatementTree> unconsumedStatements();
  }

  com.sun.tools.javac.util.List<JCStatement> inlineStatements(Inliner inliner)
      throws CouldNotResolveImportException;
}
