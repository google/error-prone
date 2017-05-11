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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.logging.Level.FINE;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.errorprone.CodeTransformer;
import com.google.errorprone.SubContext;
import com.google.errorprone.VisitorState;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.AllowCodeBetweenLines;
import com.google.errorprone.refaster.annotation.AlsoNegation;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Context;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.lang.model.element.Modifier;

/**
 * Scanner implementation to extract a single Refaster rule from a {@code ClassTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public final class RefasterRuleBuilderScanner extends SimpleTreeVisitor<Void, Void> {
  private static final Logger logger =
      Logger.getLogger(RefasterRuleBuilderScanner.class.toString());

  static final Context.Key<Map<MethodSymbol, PlaceholderMethod>> PLACEHOLDER_METHODS_KEY =
      new Context.Key<>();

  private final Context context;
  private final Map<MethodSymbol, PlaceholderMethod> placeholderMethods;
  private final List<Template<?>> beforeTemplates;
  private final List<Template<?>> afterTemplates;

  private RefasterRuleBuilderScanner(Context context) {
    this.context = new SubContext(context);
    if (context.get(PLACEHOLDER_METHODS_KEY) == null) {
      this.placeholderMethods = new HashMap<>();
      context.put(PLACEHOLDER_METHODS_KEY, placeholderMethods);
    } else {
      this.placeholderMethods = context.get(PLACEHOLDER_METHODS_KEY);
    }

    this.beforeTemplates = new ArrayList<>();
    this.afterTemplates = new ArrayList<>();
  }

  public static Collection<? extends CodeTransformer> extractRules(
      ClassTree tree, Context context) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    RefasterRuleBuilderScanner scanner = new RefasterRuleBuilderScanner(context);
    // visit abstract methods first
    List<MethodTree> methods =
        new Ordering<MethodTree>() {
          @Override
          public int compare(MethodTree l, MethodTree r) {
            return Boolean.compare(
                l.getModifiers().getFlags().contains(Modifier.ABSTRACT),
                r.getModifiers().getFlags().contains(Modifier.ABSTRACT));
          }
        }.reverse().immutableSortedCopy(Iterables.filter(tree.getMembers(), MethodTree.class));
    scanner.visit(methods, null);

    UTemplater templater = new UTemplater(context);
    List<UType> types = templater.templateTypes(sym.type.getTypeArguments());

    return scanner.createMatchers(
        Iterables.filter(types, UTypeVar.class),
        sym.getQualifiedName().toString(),
        UTemplater.annotationMap(sym));
  }

  @Override
  public Void visitMethod(MethodTree tree, Void v) {
    try {
      VisitorState state = new VisitorState(context);
      logger.log(FINE, "Discovered method with name {0}", tree.getName());
      if (ASTHelpers.hasAnnotation(tree, Placeholder.class, state)) {
        checkArgument(
            tree.getModifiers().getFlags().contains(Modifier.ABSTRACT),
            "@Placeholder methods are expected to be abstract");
        UTemplater templater = new UTemplater(context);
        ImmutableMap.Builder<UVariableDecl, ImmutableClassToInstanceMap<Annotation>> params =
            ImmutableMap.builder();
        for (VariableTree param : tree.getParameters()) {
          params.put(
              templater.visitVariable(param, null),
              UTemplater.annotationMap(ASTHelpers.getSymbol(param)));
        }
        MethodSymbol sym = ASTHelpers.getSymbol(tree);
        placeholderMethods.put(
            sym,
            PlaceholderMethod.create(
                tree.getName(),
                templater.template(sym.getReturnType()),
                params.build(),
                UTemplater.annotationMap(sym)));
      } else if (ASTHelpers.hasAnnotation(tree, BeforeTemplate.class, state)) {
        checkState(afterTemplates.isEmpty(), "BeforeTemplate must come before AfterTemplate");
        Template<?> template = UTemplater.createTemplate(context, tree);
        beforeTemplates.add(template);
        if (template instanceof BlockTemplate) {
          context.put(UTemplater.REQUIRE_BLOCK_KEY, true);
        }
      } else if (ASTHelpers.hasAnnotation(tree, AfterTemplate.class, state)) {
        afterTemplates.add(UTemplater.createTemplate(context, tree));
      } else if (tree.getModifiers().getFlags().contains(Modifier.ABSTRACT)) {
        throw new IllegalArgumentException(
            "Placeholder methods must have @Placeholder, but abstract method does not: " + tree);
      }
      return null;
    } catch (Throwable t) {
      throw new RuntimeException("Error analysing: " + tree.getName(), t);
    }
  }

  private Collection<? extends CodeTransformer> createMatchers(
      Iterable<UTypeVar> typeVars,
      String qualifiedTemplateClass,
      ImmutableClassToInstanceMap<Annotation> annotationMap) {
    if (beforeTemplates.isEmpty() && afterTemplates.isEmpty()) {
      // there's no template here
      return ImmutableList.of();
    } else {
      if (annotationMap.containsKey(AllowCodeBetweenLines.class)) {
        List<UBlank> blanks = new ArrayList<>();
        for (int i = 0; i < beforeTemplates.size(); i++) {
          BlockTemplate before = (BlockTemplate) beforeTemplates.get(i);
          List<UStatement> stmtsWithBlanks = new ArrayList<>();
          for (UStatement stmt : before.templateStatements()) {
            if (!stmtsWithBlanks.isEmpty()) {
              UBlank blank = UBlank.create();
              blanks.add(blank);
              stmtsWithBlanks.add(blank);
            }
            stmtsWithBlanks.add(stmt);
          }
          beforeTemplates.set(i, before.withStatements(stmtsWithBlanks));
        }
        for (int i = 0; i < afterTemplates.size(); i++) {
          BlockTemplate afterBlock = (BlockTemplate) afterTemplates.get(i);
          afterTemplates.set(
              i,
              afterBlock.withStatements(Iterables.concat(blanks, afterBlock.templateStatements())));
        }
      }
      RefasterRule<?, ?> rule =
          RefasterRule.create(
              qualifiedTemplateClass, typeVars, beforeTemplates, afterTemplates, annotationMap);

      List<ExpressionTemplate> negatedAfterTemplates = new ArrayList<>();
      for (Template<?> afterTemplate : afterTemplates) {
        if (afterTemplate.annotations().containsKey(AlsoNegation.class)) {
          negatedAfterTemplates.add(((ExpressionTemplate) afterTemplate).negation());
        }
      }
      if (!negatedAfterTemplates.isEmpty()) {
        List<ExpressionTemplate> negatedBeforeTemplates = new ArrayList<>();
        for (Template<?> beforeTemplate : beforeTemplates) {
          negatedBeforeTemplates.add(((ExpressionTemplate) beforeTemplate).negation());
        }
        RefasterRule<?, ?> negation =
            RefasterRule.create(
                qualifiedTemplateClass,
                typeVars,
                negatedBeforeTemplates,
                negatedAfterTemplates,
                annotationMap);
        return ImmutableList.of(rule, negation);
      }
      return ImmutableList.of(rule);
    }
  }
}
