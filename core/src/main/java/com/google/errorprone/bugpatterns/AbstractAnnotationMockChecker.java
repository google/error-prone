/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Helper for enforcing Annotations that disallow mocking.
 *
 * @param <ANNOTATION> The annotation that should not be mocked.
 */
abstract class AbstractAnnotationMockChecker<ANNOTATION extends Annotation> extends AbstractMockChecker implements
    MethodInvocationTreeMatcher, VariableTreeMatcher {

  private final Class<ANNOTATION> annotationClass;
  private final String annotationName;
  private final Function<ANNOTATION, String> getValueFunction;
  private final Supplier<MockForbidder> forbidder = Suppliers.memoize(this::forbidder);

  public AbstractAnnotationMockChecker(TypeExtractor<VariableTree> varExtractor,
      TypeExtractor<MethodInvocationTree> methodExtractor,
      Class<ANNOTATION> annotationClass, Function<ANNOTATION, String> getValueFunction) {
    super(varExtractor, methodExtractor);
    this.annotationClass = annotationClass;
    this.annotationName = annotationClass.getSimpleName();
    this.getValueFunction = getValueFunction;
  }

  @Override
  public final Description matchMethodInvocation(final MethodInvocationTree tree,
      final VisitorState state) {
    return methodExtractor
        .extract(tree, state)
        .flatMap(type -> argFromClass(type, state))
        .map(type -> checkMockedType(type, tree, state))
        .orElse(NO_MATCH);
  }

  @Override
  public final Description matchVariable(VariableTree tree, VisitorState state) {
    return varExtractor
        .extract(tree, state)
        .map(type -> checkMockedType(type, tree, state))
        .orElse(NO_MATCH);
  }

  @Override
  protected Description checkMockedType(Type mockedClass, Tree tree, VisitorState state) {
    if (ASTHelpers.isSameType(Type.noType, mockedClass, state)) {
      return NO_MATCH;
    }

    // We could extract this for loop to a "default" MockForbidder, but there's not much
    // advantage in doing so.
    for (Type currentType : Lists.reverse(state.getTypes().closure(mockedClass))) {
      TypeSymbol currentSymbol = currentType.asElement();
      ANNOTATION doNotMock = currentSymbol.getAnnotation(annotationClass);
      if (doNotMock != null) {
        return buildDescription(tree)
            .setMessage(buildMessage(mockedClass, currentSymbol, doNotMock))
            .build();
      }

      for (Compound compound : currentSymbol.getAnnotationMirrors()) {
        TypeSymbol metaAnnotationType = (TypeSymbol) compound.getAnnotationType().asElement();
        try {
          metaAnnotationType.complete();
        } catch (CompletionFailure e) {
          // if the annotation isn't on the compilation classpath, we can't check it for
          // the annotationClass meta-annotation
          continue;
        }
        doNotMock = metaAnnotationType.getAnnotation(annotationClass);
        if (doNotMock != null) {
          return buildDescription(tree)
              .setMessage(buildMessage(mockedClass, currentSymbol, metaAnnotationType, doNotMock))
              .build();
        }
      }
    }
    return forbidder
        .get()
        .forbidReason(mockedClass, state)
        .map(
            reason ->
                buildDescription(tree)
                    .setMessage(
                        buildMessage(mockedClass, reason.unmockableClass().tsym, reason.reason()))
                    .build())
        .orElse(NO_MATCH);
  }

  protected String buildMessage(Type mockedClass, TypeSymbol forbiddenType, ANNOTATION doNotMock) {
    return buildMessage(mockedClass, forbiddenType, null, doNotMock);
  }

  protected String buildMessage(
      Type mockedClass,
      TypeSymbol forbiddenType,
      @Nullable TypeSymbol metaAnnotationType,
      ANNOTATION doNotMock) {
    return String.format(
        "%s; %s is annotated as @%s%s: %s.",
        buildMessage(mockedClass, forbiddenType),
        forbiddenType,
        metaAnnotationType == null ? annotationName : metaAnnotationType,
        (metaAnnotationType == null
            ? ""
            : String.format(" (which is annotated as @%s)", annotationName)),
        Optional.ofNullable(Strings.emptyToNull(getValueFunction.apply(doNotMock)))
            .orElseGet(() -> String.format("It is annotated as %s.", annotationName)));
  }
}
