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
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of {@code ContinueTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UContinue extends USimpleStatement implements ContinueTree {
  static UContinue create(@Nullable CharSequence label) {
    return new AutoValue_UContinue((label == null) ? null : StringName.of(label));
  }

  @Override
  @Nullable
  public abstract StringName getLabel();

  @Override
  public Kind getKind() {
    return Kind.CONTINUE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitContinue(this, data);
  }

  private ULabeledStatement.Key key() {
    return new ULabeledStatement.Key(getLabel());
  }

  @Override
  public JCContinue inline(Inliner inliner) {
    return inliner.maker().Continue(ULabeledStatement.inlineLabel(getLabel(), inliner));
  }

  @Override
  public Choice<Unifier> visitContinue(ContinueTree node, Unifier unifier) {
    if (getLabel() == null) {
      return Choice.condition(node.getLabel() == null, unifier);
    } else {
      CharSequence boundName = unifier.getBinding(key());
      return Choice.condition(
          boundName != null && node.getLabel().contentEquals(boundName), unifier);
    }
  }
}
