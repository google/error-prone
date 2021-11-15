/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import static javax.lang.model.element.ElementKind.TYPE_PARAMETER;

import com.google.errorprone.util.MoreAnnotations;
import com.sun.tools.javac.code.Symbol;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/** Utilities to extract {@link Nullness} from annotations. */
public class NullnessAnnotations {
  // TODO(kmb): Correctly handle JSR 305 @Nonnull(NEVER) etc.
  private static final Predicate<String> ANNOTATION_RELEVANT_TO_NULLNESS =
      Pattern.compile(
              ".*\\b((Recently)?Nullable(Decl|Type)?|(Recently)?NotNull|NonNull(Decl|Type)?|"
                  + "Nonnull|CheckForNull|PolyNull)$")
          .asPredicate();
  private static final Predicate<String> NULLABLE_ANNOTATION =
      Pattern.compile(".*\\b((Recently)?Nullable(Decl|Type)?|CheckForNull|PolyNull)$")
          .asPredicate();

  private NullnessAnnotations() {} // static methods only

  public static Optional<Nullness> fromAnnotations(Collection<String> annotations) {
    return fromAnnotationStream(annotations.stream());
  }

  public static Optional<Nullness> fromAnnotationsOn(@Nullable Symbol sym) {
    if (sym != null) {
      return fromAnnotationStream(
          MoreAnnotations.getDeclarationAndTypeAttributes(sym).map(Object::toString));
    }
    return Optional.empty();
  }

  public static Optional<Nullness> fromAnnotationsOn(@Nullable TypeMirror type) {
    if (type != null) {
      return fromAnnotationList(type.getAnnotationMirrors());
    }
    return Optional.empty();
  }

  /**
   * Walks the syntactically enclosing elements of the given element until it finds a defaulting
   * annotation.
   */
  // Note this may be a good candidate for caching
  public static Optional<Nullness> fromDefaultAnnotations(@Nullable Element sym) {
    while (sym != null) {
      // Just look through declaration annotations here for simplicitly; default annotations aren't
      // type annotations.  For now we're just using a hard-coded simple name.
      // TODO(b/121272440): Look for existing default annotations
      if (sym.getAnnotationMirrors().stream()
          .map(Object::toString)
          .anyMatch(it -> it.endsWith(".DefaultNotNull"))) {
        return Optional.of(Nullness.NONNULL);
      }
      sym = sym.getEnclosingElement();
    }
    return Optional.empty();
  }

  /**
   * Returns any declared or implied bound for the given type variable, meaning this returns any
   * annotation on the given type variable and otherwise returns {@link #fromDefaultAnnotations} to
   * find any default in scope of the given type variable.
   */
  public static Optional<Nullness> getUpperBound(TypeVariable typeVar) {
    // Annotations on bounds at type variable declaration
    Optional<Nullness> result;
    if (typeVar.getUpperBound() instanceof IntersectionType) {
      // For intersection types, use the lower bound of any annotations on the individual bounds
      result =
          fromAnnotationStream(
              ((IntersectionType) typeVar.getUpperBound())
                  .getBounds().stream()
                      .map(TypeMirror::getAnnotationMirrors)
                      .map(Object::toString));
    } else {
      result = fromAnnotationsOn(typeVar.getUpperBound());
    }
    if (result.isPresent()) {
      // If upper bound is annotated, return that, ignoring annotations on the type variable itself.
      // This gets the upper bound for <T extends @Nullable Object> whether T is annotated or not.
      return result;
    }

    // Only if the bound isn't annotated, look for an annotation on the type variable itself and
    // treat that as the upper bound.  This handles "interface I<@NonNull|@Nullable T>" as a bound.
    if (typeVar.asElement().getKind() == TYPE_PARAMETER) {
      Element genericElt = ((TypeParameterElement) typeVar.asElement()).getGenericElement();
      if (genericElt.getKind().isClass()
          || genericElt.getKind().isInterface()
          || genericElt.getKind() == ElementKind.METHOD) {
        result =
            ((Parameterizable) genericElt)
                .getTypeParameters().stream()
                    .filter(
                        typeParam ->
                            typeParam.getSimpleName().equals(typeVar.asElement().getSimpleName()))
                    .findFirst()
                    // Annotations at class/interface/method type variable declaration
                    .flatMap(decl -> fromAnnotationList(decl.getAnnotationMirrors()));
      }
    }

    // If the type variable doesn't have an explicit bound, see if its declaration is in the scope
    // of a default and use that as the bound.
    return result.isPresent() ? result : fromDefaultAnnotations(typeVar.asElement());
  }

  private static Optional<Nullness> fromAnnotationList(List<?> annotations) {
    return fromAnnotationStream(annotations.stream().map(Object::toString));
  }

  private static Optional<Nullness> fromAnnotationStream(Stream<String> annotations) {
    return annotations
        .filter(ANNOTATION_RELEVANT_TO_NULLNESS)
        .map(annot -> NULLABLE_ANNOTATION.test(annot) ? Nullness.NULLABLE : Nullness.NONNULL)
        .reduce(Nullness::greatestLowerBound);
  }
}
