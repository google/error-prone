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
import javax.lang.model.element.ExecutableElement;
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
              ".*\\b("
                  + "(Recently)?NotNull|NonNull(Decl|Type)?|Nonnull|"
                  + "(Recently)?Nullable(Decl|Type)?|CheckForNull|PolyNull|MonotonicNonNull(Decl)?|"
                  + "ProtoMethodMayReturnNull|ProtoMethodAcceptsNullParameter|"
                  + "ProtoPassThroughNullness"
                  + ")$")
          .asPredicate();
  private static final Predicate<String> NULLABLE_ANNOTATION =
      Pattern.compile(
              ".*\\b("
                  + "(Recently)?Nullable(Decl|Type)?|CheckForNull|PolyNull|MonotonicNonNull(Decl)?|"
                  + "ProtoMethodMayReturnNull|ProtoMethodAcceptsNullParameter|"
                  + "ProtoPassThroughNullness"
                  + ")$")
          .asPredicate();

  private NullnessAnnotations() {} // static methods only

  public static Optional<Nullness> fromAnnotations(Collection<String> annotations) {
    return fromAnnotationStream(annotations.stream());
  }

  public static Optional<Nullness> fromAnnotationsOn(@Nullable Symbol sym) {
    if (sym == null) {
      return Optional.empty();
    }

    /*
     * We try to read annotations in two ways:
     *
     * 1. from the TypeMirror: This is how we "should" always read *type-use* annotations, but
     * JDK-8225377 prevents it from working across compilation boundaries.
     *
     * 2. from getRawAttributes(): This works around the problem across compilation boundaries, and
     * it handles declaration annotations (though there are other ways we could handle declaration
     * annotations). But it has a bug of its own with type-use annotations on inner classes
     * (b/203207989). To reduce the chance that we hit the inner-class bug, we apply it only if the
     * first approach fails.
     */
    TypeMirror elementType;
    switch (sym.getKind()) {
      case METHOD:
        elementType = ((ExecutableElement) sym).getReturnType();
        break;
      case FIELD:
      case PARAMETER:
        elementType = sym.asType();
        break;
      default:
        elementType = null;
    }
    Optional<Nullness> fromElement = fromAnnotationsOn(elementType);
    if (fromElement.isPresent()) {
      return fromElement;
    }

    return fromAnnotationStream(
        MoreAnnotations.getDeclarationAndTypeAttributes(sym).map(Object::toString));
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
