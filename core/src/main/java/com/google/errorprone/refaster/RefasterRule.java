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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.CodeTransformer;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.SubContext;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.tools.JavaFileManager;

/**
 * A representation of an entire Refaster rule, corresponding to a class with @BeforeTemplates
 * and @AfterTemplates.
 *
 * @author lowasser@google.com (Louis Wasserman)
 * @param <M> The type of a match.
 * @param <T> The type of the template used to find matches and generate replacements.
 */
@AutoValue
public abstract class RefasterRule<M extends TemplateMatch, T extends Template<M>>
    implements CodeTransformer, Serializable {
  public static RefasterRule<?, ?> create(
      String qualifiedTemplateClass,
      Collection<? extends Template<?>> beforeTemplates,
      Collection<? extends Template<?>> afterTemplates) {
    return create(
        qualifiedTemplateClass,
        ImmutableList.<UTypeVar>of(),
        beforeTemplates,
        afterTemplates,
        ImmutableClassToInstanceMap.<Annotation>builder().build());
  }

  public static RefasterRule<?, ?> create(
      String qualifiedTemplateClass,
      Iterable<UTypeVar> typeVariables,
      Collection<? extends Template<?>> beforeTemplates,
      Collection<? extends Template<?>> afterTemplates,
      ImmutableClassToInstanceMap<Annotation> annotations) {

    checkState(
        !beforeTemplates.isEmpty(),
        "No @BeforeTemplate was found in the specified class: %s",
        qualifiedTemplateClass);
    Class<?> templateType = beforeTemplates.iterator().next().getClass();
    for (Template<?> beforeTemplate : beforeTemplates) {
      checkState(
          beforeTemplate.getClass().equals(templateType),
          "Expected all templates to be of type %s but found template of type %s in %s",
          templateType,
          beforeTemplate.getClass(),
          qualifiedTemplateClass);
    }
    for (Template<?> afterTemplate : afterTemplates) {
      checkState(
          afterTemplate.getClass().equals(templateType),
          "Expected all templates to be of type %s but found template of type %s in %s",
          templateType,
          afterTemplate.getClass(),
          qualifiedTemplateClass);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    RefasterRule<?, ?> result =
        new AutoValue_RefasterRule(
            qualifiedTemplateClass,
            ImmutableList.copyOf(typeVariables),
            ImmutableList.copyOf(beforeTemplates),
            ImmutableList.copyOf(afterTemplates),
            annotations);
    return result;
  }

  RefasterRule() {}

  abstract String qualifiedTemplateClass();

  abstract ImmutableList<UTypeVar> typeVariables();

  abstract ImmutableList<T> beforeTemplates();

  @Nullable
  abstract ImmutableList<T> afterTemplates();

  @Override
  public abstract ImmutableClassToInstanceMap<Annotation> annotations();

  @Override
  public void apply(TreePath path, Context context, DescriptionListener listener) {
    RefasterScanner.create(this, listener)
        .scan(path, prepareContext(context, (JCCompilationUnit) path.getCompilationUnit()));
  }

  boolean rejectMatchesWithComments() {
    return true; // TODO(lowasser): worth making configurable?
  }

  static final Context.Key<ImmutableList<UTypeVar>> RULE_TYPE_VARS = new Context.Key<>();

  private Context prepareContext(Context baseContext, JCCompilationUnit compilationUnit) {
    Context context = new SubContext(baseContext);
    if (context.get(JavaFileManager.class) == null) {
      JavacFileManager.preRegister(context);
    }
    context.put(JCCompilationUnit.class, compilationUnit);
    context.put(PackageSymbol.class, compilationUnit.packge);
    context.put(RULE_TYPE_VARS, typeVariables());
    return context;
  }

  @VisibleForTesting
  static String fromSecondLevel(String qualifiedTemplateClass) {
    List<String> path = Splitter.on('.').splitToList(qualifiedTemplateClass);
    for (int topLevel = 0; topLevel < path.size() - 1; topLevel++) {
      if (Ascii.isUpperCase(path.get(topLevel).charAt(0))) {
        return Joiner.on('_').join(path.subList(topLevel + 1, path.size()));
      }
    }
    return Iterables.getLast(path);
  }

  @Override
  public String toString() {
    return fromSecondLevel(qualifiedTemplateClass());
  }
}
