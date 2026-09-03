/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.errorprone.CodeTransformer;
import com.google.errorprone.DescriptionListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

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
        ImmutableClassToInstanceMap.of());
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
      Set<String> missingArguments =
          Sets.difference(
              afterTemplate.expressionArgumentTypes().keySet(),
              beforeTemplates.stream()
                  .<Set<String>>map(t -> t.expressionArgumentTypes().keySet())
                  .reduce(Sets::intersection)
                  .get());
      checkArgument(
          missingArguments.isEmpty(),
          "@AfterTemplate of %s defines arguments that are not present in all @BeforeTemplate"
              + " methods: %s",
          qualifiedTemplateClass,
          missingArguments);

      checkState(
          afterTemplate.getClass().equals(templateType),
          "Expected all templates to be of type %s but found template of type %s in %s",
          templateType,
          afterTemplate.getClass(),
          qualifiedTemplateClass);
    }
    ImmutableList<UTypeVar> typeVars = ImmutableList.copyOf(typeVariables);
    ImmutableList<Template<?>> before =
        beforeTemplates.stream()
            .map(t -> t.withRuleTypeVariables(typeVars))
            .collect(toImmutableList());
    ImmutableList<Template<?>> after =
        afterTemplates.stream()
            .map(t -> t.withRuleTypeVariables(typeVars))
            .collect(toImmutableList());
    @SuppressWarnings({"unchecked", "rawtypes"})
    RefasterRule<?, ?> result =
        new AutoValue_RefasterRule(
            qualifiedTemplateClass,
            fromSecondLevel(qualifiedTemplateClass),
            typeVars,
            before,
            after,
            annotations);
    return result;
  }

  RefasterRule() {}

  public abstract String qualifiedTemplateClass();

  abstract String simpleTemplateName();

  abstract ImmutableList<UTypeVar> typeVariables();

  abstract ImmutableList<T> beforeTemplates();

  abstract ImmutableList<T> afterTemplates();

  @Override
  public abstract ImmutableClassToInstanceMap<Annotation> annotations();

  @Override
  public void apply(TreePath path, Context context, DescriptionListener listener) {
    JCCompilationUnit compilationUnit = (JCCompilationUnit) path.getCompilationUnit();
    RefasterScanner.create(this, listener, compilationUnit).scan(path.getLeaf(), context);
  }

  boolean rejectMatchesWithComments() {
    return true; // TODO: b/12365776 - Make this option configurable.
  }

  @VisibleForTesting
  static String fromSecondLevel(String qualifiedTemplateClass) {
    int start = 0;
    int dot;
    while ((dot = qualifiedTemplateClass.indexOf('.', start)) != -1) {
      if (Ascii.isUpperCase(qualifiedTemplateClass.charAt(start))) {
        return qualifiedTemplateClass.substring(dot + 1).replace('.', '_');
      }
      start = dot + 1;
    }
    return qualifiedTemplateClass.substring(start);
  }

  @Override
  public final String toString() {
    return simpleTemplateName();
  }
}
