/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject;

import static com.google.auto.common.MoreElements.asType;
import static javax.lang.model.util.ElementFilter.constructorsIn;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.common.base.Optional;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/** Predicates for {@link Element} objects related to dependency injection. */
public final class ElementPredicates {

  public static boolean isFinalField(Element element) {
    return element.getKind().equals(ElementKind.FIELD)
        && element.getModifiers().contains(Modifier.FINAL);
  }

  public static boolean isFirstConstructorOfMultiInjectedClass(Element injectedMember) {
    if (injectedMember.getKind() == ElementKind.CONSTRUCTOR) {
      List<ExecutableElement> injectConstructors =
          getConstructorsWithAnnotations(
              injectedMember, Arrays.asList("javax.inject.Inject", "com.google.inject.Inject"));
      if (injectConstructors.size() > 1 && injectConstructors.get(0).equals(injectedMember)) {
        return true;
      }
    }
    return false;
  }

  public static boolean doesNotHaveRuntimeRetention(Element element) {
    Optional<AnnotationMirror> annotationMirror =
        MoreElements.getAnnotationMirror(element, Retention.class);
    // Default retention is CLASS, not RUNTIME, so return true if retention is missing
    if (annotationMirror.isPresent()) {
      AnnotationValue annotationValue =
          AnnotationMirrors.getAnnotationValue(annotationMirror.get(), "value");
      if (annotationValue.getValue().toString().equals(RetentionPolicy.RUNTIME.toString())) {
        return false;
      }
    }
    return true;
  }

  private static List<ExecutableElement> getConstructorsWithAnnotations(
      Element exploringConstructor, List<String> annotations) {
    return constructorsIn(exploringConstructor.getEnclosingElement().getEnclosedElements())
        .stream()
        .filter(constructor -> hasAnyOfAnnotation(constructor, annotations))
        .sorted(Comparator.comparing((e -> e.getSimpleName().toString())))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static boolean hasAnyOfAnnotation(ExecutableElement input, List<String> annotations) {
    return input
        .getAnnotationMirrors()
        .stream()
        .map(annotationMirror -> asType(annotationMirror.getAnnotationType().asElement()))
        .anyMatch(type -> typeInAnnotations(type, annotations));
  }

  private static boolean typeInAnnotations(TypeElement t, List<String> annotations) {
    return annotations
        .stream()
        .anyMatch(annotation -> t.getQualifiedName().contentEquals(annotation));
  }
}
