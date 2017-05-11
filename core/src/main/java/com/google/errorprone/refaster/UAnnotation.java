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

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.List;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link AnnotationTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UAnnotation extends UExpression implements AnnotationTree {
  public static UAnnotation create(UTree<?> annotationType, List<UExpression> arguments) {
    return new AutoValue_UAnnotation(annotationType, ImmutableList.copyOf(arguments));
  }

  public static UAnnotation create(UTree<?> annotationType, UExpression... arguments) {
    return create(annotationType, ImmutableList.copyOf(arguments));
  }

  @Override
  public abstract UTree<?> getAnnotationType();

  @Override
  public abstract List<UExpression> getArguments();

  @Override
  @Nullable
  public Choice<Unifier> visitAnnotation(AnnotationTree annotation, Unifier unifier) {
    return getAnnotationType()
        .unify(annotation.getAnnotationType(), unifier)
        .thenChoose(unifications(getArguments(), annotation.getArguments()));
  }

  @Override
  public Kind getKind() {
    return Kind.ANNOTATION;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitAnnotation(this, data);
  }

  @Override
  public JCAnnotation inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Annotation(
            getAnnotationType().inline(inliner), inliner.<JCExpression>inlineList(getArguments()));
  }
}
