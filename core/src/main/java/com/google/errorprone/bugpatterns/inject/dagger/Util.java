/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.common.collect.Iterables.transform;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.hasMethod;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.createPrivateConstructor;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Matchers and utilities useful to Dagger bug checkers. */
final class Util {

  private Util() {}

  static final Matcher<Tree> ANNOTATED_WITH_PRODUCES_OR_PROVIDES =
      anyOf(hasAnnotation("dagger.Provides"), hasAnnotation("dagger.producers.Produces"));

  static final Matcher<Tree> ANNOTATED_WITH_MULTIBINDING_ANNOTATION =
      anyOf(
          hasAnnotation("dagger.multibindings.IntoSet"),
          hasAnnotation("dagger.multibindings.ElementsIntoSet"),
          hasAnnotation("dagger.multibindings.IntoMap"));

  /**
   * Matches Dagger 2 {@linkplain dagger.Module modules} and {@linkplain
   * dagger.producers.ProducersModule producer modules}.
   */
  static final Matcher<Tree> IS_DAGGER_2_MODULE =
      annotations(
          AT_LEAST_ONE,
          anyOf(
              allOf(
                  isType("dagger.Module"),
                  not(
                      hasAnyParameter(
                          "injects",
                          "staticInjections",
                          "overrides",
                          "addsTo",
                          "complete",
                          "library"))),
              isType("dagger.producers.ProducerModule")));

  /** Matches an annotation that has an argument for at least one of the given parameters. */
  private static Matcher<AnnotationTree> hasAnyParameter(String... parameters) {
    return anyOf(
        transform(
            asList(parameters),
            new Function<String, Matcher<AnnotationTree>>() {
              @Override
              public Matcher<AnnotationTree> apply(String parameter) {
                return hasArgumentWithValue(parameter, Matchers.<ExpressionTree>anything());
              }
            }));
  }

  private static final Matcher<ClassTree> CLASS_EXTENDS_NOTHING =
      new Matcher<ClassTree>() {
        @Override
        public boolean matches(ClassTree t, VisitorState state) {
          return t.getExtendsClause() == null;
        }
      };

  /**
   * Matches Dagger 2 {@linkplain dagger.Module modules} and {@linkplain
   * dagger.producers.ProducersModule producer modules} that could contain abstract binding methods.
   *
   * <ul>
   *   <li>an interface or a class with no superclass
   *   <li>no instance {@link dagger.Provides} or {@link dagger.producers.Produces} methods
   * </ul>
   */
  static final Matcher<ClassTree> CAN_HAVE_ABSTRACT_BINDING_METHODS =
      allOf(
          IS_DAGGER_2_MODULE,
          anyOf(kindIs(INTERFACE), CLASS_EXTENDS_NOTHING),
          not(
              hasMethod(
                  Matchers.<MethodTree>allOf(
                      ANNOTATED_WITH_PRODUCES_OR_PROVIDES, not(hasModifier(STATIC))))));

  /** Returns the annotation on {@code classTree} whose type's FQCN is {@code annotationName}. */
  static Optional<AnnotationTree> findAnnotation(String annotationName, ClassTree classTree) {
    for (AnnotationTree annotationTree : classTree.getModifiers().getAnnotations()) {
      ClassSymbol annotationClass = (ClassSymbol) getSymbol(annotationTree.getAnnotationType());
      if (annotationClass.fullname.contentEquals(annotationName)) {
        return Optional.of(annotationTree);
      }
    }
    return Optional.absent();
  }

  private static final MultiMatcher<ClassTree, MethodTree> HAS_GENERATED_CONSTRUCTOR =
      constructor(
          AT_LEAST_ONE,
          new Matcher<MethodTree>() {
            @Override
            public boolean matches(MethodTree t, VisitorState state) {
              return isGeneratedConstructor(t);
            }
          });

  /**
   * Returns a fix that changes a concrete class to an abstract class.
   *
   * <ul>
   *   <li>Removes {@code final} if it was there.
   *   <li>Adds {@code abstract} if it wasn't there.
   *   <li>Adds a private empty constructor if the class was {@code final} and had only a default
   *       constructor.
   * </ul>
   */
  static SuggestedFix.Builder makeConcreteClassAbstract(ClassTree classTree, VisitorState state) {
    Set<Modifier> flags = EnumSet.noneOf(Modifier.class);
    flags.addAll(classTree.getModifiers().getFlags());
    boolean wasFinal = flags.remove(FINAL);
    boolean wasAbstract = !flags.add(ABSTRACT);

    if (classTree.getKind().equals(INTERFACE) || (!wasFinal && wasAbstract)) {
      return SuggestedFix.builder(); // no-op
    }

    ImmutableList.Builder<Object> modifiers = ImmutableList.builder();
    for (AnnotationTree annotation : classTree.getModifiers().getAnnotations()) {
      modifiers.add(state.getSourceForNode(annotation));
    }
    modifiers.addAll(flags);

    SuggestedFix.Builder makeAbstract = SuggestedFix.builder();
    if (((JCModifiers) classTree.getModifiers()).pos == -1) {
      makeAbstract.prefixWith(classTree, Joiner.on(' ').join(modifiers.build()));
    } else {
      makeAbstract.replace(classTree.getModifiers(), Joiner.on(' ').join(modifiers.build()));
    }
    if (wasFinal && HAS_GENERATED_CONSTRUCTOR.matches(classTree, state)) {
      makeAbstract.merge(addPrivateConstructor(classTree));
    }
    return makeAbstract;
  }

  // TODO(dpb): Account for indentation level.
  private static SuggestedFix.Builder addPrivateConstructor(ClassTree classTree) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String indent = "  ";
    for (Tree member : classTree.getMembers()) {
      if (member.getKind().equals(METHOD) && !isGeneratedConstructor((MethodTree) member)) {
        fix.prefixWith(
            member, indent + createPrivateConstructor(classTree) + " // no instances\n" + indent);
        break;
      }
      if (!member.getKind().equals(METHOD)) {
        indent = "";
      }
    }
    return fix;
  }
}
