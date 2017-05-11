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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * {@code UTree} representation of a {@code ModifiersTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UModifiers extends UTree<JCModifiers> implements ModifiersTree {
  public static UModifiers create(long flagBits, UAnnotation... annotations) {
    return create(flagBits, ImmutableList.copyOf(annotations));
  }

  public static UModifiers create(long flagBits, Iterable<? extends UAnnotation> annotations) {
    return new AutoValue_UModifiers(flagBits, ImmutableList.copyOf(annotations));
  }

  abstract long flagBits();

  @Override
  public abstract ImmutableList<UAnnotation> getAnnotations();

  @Override
  public JCModifiers inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Modifiers(
            flagBits(), List.convert(JCAnnotation.class, inliner.inlineList(getAnnotations())));
  }

  @Override
  public Choice<Unifier> visitModifiers(ModifiersTree modifier, Unifier unifier) {
    return Choice.condition(getFlags().equals(modifier.getFlags()), unifier);
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitModifiers(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.MODIFIERS;
  }

  @Override
  public Set<Modifier> getFlags() {
    return Flags.asModifierSet(flagBits());
  }
}
