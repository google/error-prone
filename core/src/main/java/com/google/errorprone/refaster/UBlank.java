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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.errorprone.refaster.UStatement.UnifierWithUnconsumedStatements;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.ListBuffer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Equivalent to a no-arg block placeholder invocation. */
@AutoValue
abstract class UBlank implements UStatement {
  static UBlank create() {
    return new AutoValue_UBlank(UUID.randomUUID());
  }

  abstract UUID unique();

  Key key() {
    return new Key(unique());
  }

  static class Key extends Bindings.Key<List<? extends StatementTree>> {
    Key(UUID k) {
      super(k.toString());
    }
  }

  private static final TreeScanner<Boolean, Unifier> FORBIDDEN_REFERENCE_SCANNER =
      new TreeScanner<Boolean, Unifier>() {
        @Override
        public Boolean reduce(Boolean l, Boolean r) {
          return firstNonNull(l, false) || firstNonNull(r, false);
        }

        @Override
        public Boolean scan(Tree t, Unifier unifier) {
          if (t != null) {
            Boolean forbidden =
                t.accept(PlaceholderUnificationVisitor.FORBIDDEN_REFERENCE_VISITOR, unifier);
            return firstNonNull(forbidden, false) || firstNonNull(super.scan(t, unifier), false);
          }
          return false;
        }
      };

  @Override
  public Tree.Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitOther(this, data);
  }

  @Override
  public Choice<UnifierWithUnconsumedStatements> apply(
      final UnifierWithUnconsumedStatements state) {
    int goodIndex = 0;
    while (goodIndex < state.unconsumedStatements().size()) {
      StatementTree stmt = state.unconsumedStatements().get(goodIndex);
      if (firstNonNull(FORBIDDEN_REFERENCE_SCANNER.scan(stmt, state.unifier()), false)
          && ControlFlowVisitor.INSTANCE.visitStatement(stmt)
              == ControlFlowVisitor.Result.NEVER_EXITS) {
        break;
      } else {
        goodIndex++;
      }
    }
    Collection<Integer> breakPoints =
        ContiguousSet.create(Range.closed(0, goodIndex), DiscreteDomain.integers());
    return Choice.from(breakPoints)
        .transform(
            (Integer k) -> {
              Unifier unifier = state.unifier().fork();
              unifier.putBinding(key(), state.unconsumedStatements().subList(0, k));
              List<? extends StatementTree> remaining =
                  state.unconsumedStatements().subList(k, state.unconsumedStatements().size());
              return UnifierWithUnconsumedStatements.create(unifier, remaining);
            });
  }

  @Override
  public com.sun.tools.javac.util.List<JCStatement> inlineStatements(Inliner inliner) {
    ListBuffer<JCStatement> buffer = new ListBuffer<>();
    for (StatementTree stmt :
        inliner.getOptionalBinding(key()).or(ImmutableList.<StatementTree>of())) {
      buffer.add((JCStatement) stmt);
    }
    return buffer.toList();
  }
}
