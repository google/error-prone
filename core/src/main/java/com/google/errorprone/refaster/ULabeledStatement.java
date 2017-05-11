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

import com.google.auto.value.AutoValue;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.util.Name;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of a {@code LabeledStatementTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class ULabeledStatement extends USimpleStatement implements LabeledStatementTree {
  static ULabeledStatement create(CharSequence label, UStatement statement) {
    return new AutoValue_ULabeledStatement(StringName.of(label), (USimpleStatement) statement);
  }

  static class Key extends Bindings.Key<CharSequence> {
    Key(CharSequence identifier) {
      super(identifier.toString());
    }
  }

  /**
   * Returns either the {@code Name} bound to the specified label, or a {@code Name} representing
   * the original label if none is already bound.
   */
  @Nullable
  static Name inlineLabel(@Nullable CharSequence label, Inliner inliner) {
    return (label == null)
        ? null
        : inliner.asName(inliner.getOptionalBinding(new Key(label)).or(label));
  }

  @Override
  public abstract StringName getLabel();

  @Override
  public abstract USimpleStatement getStatement();

  @Override
  public Kind getKind() {
    return Kind.LABELED_STATEMENT;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitLabeledStatement(this, data);
  }

  private Key key() {
    return new Key(getLabel());
  }

  @Override
  public JCLabeledStatement inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Labelled(inlineLabel(getLabel(), inliner), getStatement().inline(inliner));
  }

  @Override
  public Choice<Unifier> visitLabeledStatement(LabeledStatementTree node, Unifier unifier) {
    unifier.putBinding(key(), node.getLabel());
    return getStatement().unify(node.getStatement(), unifier);
  }
}
