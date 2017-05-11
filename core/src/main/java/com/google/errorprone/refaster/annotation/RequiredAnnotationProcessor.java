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

package com.google.errorprone.refaster.annotation;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import javax.tools.Diagnostic.Kind;

/**
 * Enforces {@code @RequiredAnnotation} as an annotation processor.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@SupportedAnnotationTypes("*")
public final class RequiredAnnotationProcessor extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      return false;
    }
    validateElements(roundEnv.getRootElements());
    return false;
  }

  private AnnotationMirror getAnnotationMirror(Element element, TypeMirror annotationType) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (processingEnv.getTypeUtils().isSameType(mirror.getAnnotationType(), annotationType)) {
        return mirror;
      }
    }
    return null;
  }

  private AnnotationValue getAnnotationValue(AnnotationMirror mirror, String key) {
    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        mirror.getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(key)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private void validateElements(Iterable<? extends Element> elements) {
    for (Element element : elements) {
      validateElement(element);
    }
  }

  private void validateElement(final Element element) {
    TypeMirror requiredAnnotationTypeMirror =
        processingEnv.getElementUtils().getTypeElement(RequiredAnnotation.class.getName()).asType();
    for (final AnnotationMirror annotation :
        processingEnv.getElementUtils().getAllAnnotationMirrors(element)) {
      AnnotationMirror requiredAnnotationMirror =
          getAnnotationMirror(
              annotation.getAnnotationType().asElement(), requiredAnnotationTypeMirror);
      if (requiredAnnotationMirror == null) {
        continue;
      }
      AnnotationValue value = getAnnotationValue(requiredAnnotationMirror, "value");
      if (value == null) {
        continue;
      }
      new SimpleAnnotationValueVisitor7<Void, Void>() {
        @Override
        public Void visitType(TypeMirror t, Void p) {
          if (getAnnotationMirror(element, t) == null) {
            printError(
                element,
                annotation,
                "Annotation %s on %s also requires %s",
                annotation,
                element,
                t);
          }
          return null;
        }

        @Override
        public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
          for (AnnotationValue val : vals) {
            visit(val);
          }
          return null;
        }
      }.visit(value);
    }
    validateElements(element.getEnclosedElements());
  }

  private void printError(
      Element element, AnnotationMirror annotation, String message, Object... args) {
    processingEnv
        .getMessager()
        .printMessage(Kind.ERROR, String.format(message, args), element, annotation);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }
}
