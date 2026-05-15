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

package com.google.errorprone.bugpatterns.inject;

import static com.google.auto.common.MoreElements.asType;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static javax.lang.model.util.ElementFilter.constructorsIn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.matchers.InjectMatchers;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.List;
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
          getConstructorsWithAnnotations(injectedMember, InjectMatchers.INJECT_ANNOTATIONS);
      if (injectConstructors.size() > 1 && injectConstructors.getFirst().equals(injectedMember)) {
        return true;
      }
    }
    return false;
  }

  public static boolean doesNotHaveRuntimeRetention(Element element) {
    return effectiveRetentionPolicy(element) != RetentionPolicy.RUNTIME;
  }

  public static boolean hasSourceRetention(Element element) {
    return effectiveRetentionPolicy(element) == RetentionPolicy.SOURCE;
  }

  private static RetentionPolicy effectiveRetentionPolicy(Element element) {
    RetentionPolicy retentionPolicy = RetentionPolicy.CLASS;
    Retention retentionAnnotation = element.getAnnotation(Retention.class);
    if (retentionAnnotation != null) {
      retentionPolicy = retentionAnnotation.value();
    }
    return retentionPolicy;
  }

  private static ImmutableList<ExecutableElement> getConstructorsWithAnnotations(
      Element exploringConstructor, ImmutableSet<String> annotations) {
    return constructorsIn(exploringConstructor.getEnclosingElement().getEnclosedElements()).stream()
        .filter(constructor -> hasAnyOfAnnotation(constructor, annotations))
        .sorted(Comparator.comparing((e -> e.getSimpleName().toString())))
        .collect(toImmutableList());
  }

  private static boolean hasAnyOfAnnotation(
      ExecutableElement input, ImmutableSet<String> annotations) {
    return input.getAnnotationMirrors().stream()
        .map(annotationMirror -> asType(annotationMirror.getAnnotationType().asElement()))
        .anyMatch(type -> typeInAnnotations(type, annotations));
  }

  private static boolean typeInAnnotations(TypeElement t, ImmutableSet<String> annotations) {
    return annotations.stream()
        .anyMatch(annotation -> t.getQualifiedName().contentEquals(annotation));
  }

  private ElementPredicates() {}
}
