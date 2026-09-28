/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.util.AnnotationNames.RETENTION_ANNOTATION;
import static com.google.errorprone.util.AnnotationNames.TARGET_ANNOTATION;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.InlineMe;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import org.jspecify.annotations.Nullable;

/** Annotation-related utilities. */
public final class MoreAnnotations {

  /**
   * Returns the annotation on the given element with the specified qualified binary or canonical
   * name.
   */
  public static Optional<Attribute.Compound> getAnnotation(Element sym, String qualifiedName) {
    if (!(sym instanceof Symbol symbol)) {
      return Optional.empty();
    }
    for (Attribute.Compound a : symbol.getRawAttributes()) {
      if (a.type.tsym.getQualifiedName().contentEquals(qualifiedName)) {
        return Optional.of(a);
      }
    }
    return Optional.empty();
  }

  /** Returns the annotation on the given element with the specified qualified name. */
  public static Optional<Attribute.Compound> getAnnotation(Element sym, Name qualifiedName) {
    if (!(sym instanceof Symbol symbol)) {
      return Optional.empty();
    }
    for (Attribute.Compound a : symbol.getRawAttributes()) {
      if (a.type.tsym.getQualifiedName().equals(qualifiedName)) {
        return Optional.of(a);
      }
    }
    return Optional.empty();
  }

  /** Returns the annotation on the given element with the specified simple name. */
  public static Optional<Attribute.Compound> getAnnotationWithSimpleName(
      Element sym, String simpleName) {
    if (!(sym instanceof Symbol symbol)) {
      return Optional.empty();
    }
    for (Attribute.Compound a : symbol.getRawAttributes()) {
      if (a.type.tsym.getSimpleName().contentEquals(simpleName)) {
        return Optional.of(a);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the value of the annotation element-value pair with the given name if it is explicitly
   * set.
   */
  public static Optional<AnnotationValue> getValue(AnnotationMirror annotationMirror, String name) {
    if (annotationMirror == null) {
      return Optional.empty();
    }
    return annotationMirror.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().contentEquals(name))
        .<AnnotationValue>map(Map.Entry::getValue)
        .findFirst();
  }

  /**
   * Returns the value of the annotation element-value pair with the given name if it is explicitly
   * set.
   *
   * @deprecated Prefer {@link #getValue(AnnotationMirror, String)}.
   */
  @InlineMe(
      replacement =
          "MoreAnnotations.getValue((AnnotationMirror) attribute, name).map(Attribute.class::cast)",
      imports = {
        "com.google.errorprone.util.MoreAnnotations",
        "com.sun.tools.javac.code.Attribute",
        "javax.lang.model.element.AnnotationMirror"
      })
  @Deprecated
  public static Optional<Attribute> getValue(Attribute.Compound attribute, String name) {
    return getValue((AnnotationMirror) attribute, name).map(Attribute.class::cast);
  }

  /** Shorthand for extracting a string value of a named element from an annotation mirror. */
  public static Optional<String> getStringValue(AnnotationMirror attribute, String name) {
    return getValue(attribute, name).flatMap(MoreAnnotations::asStringValue);
  }

  /** Shorthand for extracting the "value" string of a named annotation on an element. */
  public static Optional<String> getStringValue(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName).flatMap(a -> getStringValue(a, "value"));
  }

  /** Shorthand for extracting a boolean value of a named element from an annotation mirror. */
  public static Optional<Boolean> getBooleanValue(AnnotationMirror attribute, String name) {
    return getValue(attribute, name).flatMap(MoreAnnotations::asBooleanValue);
  }

  /** Shorthand for extracting the "value" boolean of a named annotation on an element. */
  public static Optional<Boolean> getBooleanValue(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName).flatMap(a -> getBooleanValue(a, "value"));
  }

  /** Shorthand for extracting an enum value of a named element from an annotation mirror. */
  public static Optional<VarSymbol> getEnumValue(AnnotationMirror attribute, String name) {
    return getValue(attribute, name).flatMap(MoreAnnotations::asEnumValue);
  }

  /** Shorthand for extracting the "value" enum of a named annotation on an element. */
  public static Optional<VarSymbol> getEnumValue(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName).flatMap(a -> getEnumValue(a, "value"));
  }

  /** Shorthand for extracting enum values of a named element from an annotation mirror. */
  public static ImmutableList<VarSymbol> getEnumValues(AnnotationMirror attribute, String name) {
    return getValue(attribute, name)
        .map(v -> asEnumValues(v).collect(toImmutableList()))
        .orElseGet(ImmutableList::of);
  }

  /** Shorthand for extracting the "value" enum values of a named annotation on an element. */
  public static ImmutableList<VarSymbol> getEnumValues(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName)
        .map(a -> getEnumValues(a, "value"))
        .orElseGet(ImmutableList::of);
  }

  /**
   * Shorthand for extracting a list of strings for a named element (e.g. array or single string).
   */
  public static ImmutableList<String> getStrings(AnnotationMirror attribute, String name) {
    return getValue(attribute, name)
        .map(v -> asStrings(v).collect(toImmutableList()))
        .orElseGet(ImmutableList::of);
  }

  /**
   * Shorthand for extracting a list of strings from the "value" element of a named annotation on an
   * element.
   */
  public static ImmutableList<String> getStrings(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName)
        .map(a -> getStrings(a, "value"))
        .orElseGet(ImmutableList::of);
  }

  /** Shorthand for extracting a list of types for a named element (e.g. Class[] or Class). */
  public static ImmutableList<TypeMirror> getTypes(AnnotationMirror attribute, String name) {
    return getValue(attribute, name)
        .map(v -> asTypes(v).collect(toImmutableList()))
        .orElseGet(ImmutableList::of);
  }

  /**
   * Shorthand for extracting a list of types from the "value" element of a named annotation on an
   * element.
   */
  public static ImmutableList<TypeMirror> getTypes(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName)
        .map(a -> getTypes(a, "value"))
        .orElseGet(ImmutableList::of);
  }

  /** Shorthand for extracting a type value of a named element from an annotation mirror. */
  public static Optional<TypeMirror> getTypeValue(AnnotationMirror attribute, String name) {
    return getValue(attribute, name).flatMap(MoreAnnotations::asTypeValue);
  }

  /** Shorthand for extracting the "value" type of a named annotation on an element. */
  public static Optional<TypeMirror> getTypeValue(Element sym, String annotationName) {
    return getAnnotation(sym, annotationName).flatMap(a -> getTypeValue(a, "value"));
  }

  /** Converts the given attribute to a boolean value. */
  public static Optional<Boolean> asBooleanValue(AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<Boolean, Void> {
      @Override
      public Boolean visitBoolean(boolean b, Void unused) {
        return b;
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
  }

  /** Converts the given attribute to an integer value. */
  public static Optional<Integer> asIntegerValue(AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<Integer, Void> {
      @Override
      public Integer visitInt(int i, Void unused) {
        return i;
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
  }

  /** Converts the given attribute to a string value. */
  public static Optional<String> asStringValue(AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<String, Void> {
      @Override
      public String visitString(String s, Void unused) {
        return s;
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
  }

  /** Converts the given attribute to an enum value. */
  public static Optional<VarSymbol> asEnumValue(AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<VarSymbol, Void> {
      @Override
      public VarSymbol visitEnumConstant(VariableElement c, Void unused) {
        return (VarSymbol) c;
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
  }

  /**
   * Converts the given attribute to an enum value.
   *
   * @deprecated Prefer {@link #asEnumValue(AnnotationValue)}.
   */
  @Deprecated
  public static <T extends Enum<T>> Optional<T> asEnumValue(Class<T> clazz, AnnotationValue a) {
    return asEnumValue(a).map(c -> Enum.valueOf(clazz, c.getSimpleName().toString()));
  }

  /** Converts the given annotation value to one or more enum values. */
  public static Stream<VarSymbol> asEnumValues(AnnotationValue v) {
    return v.accept(
        new SimpleAnnotationValueVisitor8<Stream<VarSymbol>, Void>(Stream.empty()) {
          @Override
          public Stream<VarSymbol> visitEnumConstant(VariableElement c, Void unused) {
            return Stream.of((VarSymbol) c);
          }

          @Override
          public Stream<VarSymbol> visitArray(List<? extends AnnotationValue> list, Void unused) {
            return list.stream().flatMap(a -> a.accept(this, null));
          }
        },
        null);
  }

  /**
   * Converts the given attribute to enum values.
   *
   * @deprecated Prefer {@link #asEnumValues(AnnotationValue)}.
   */
  @Deprecated
  public static <T extends Enum<T>> EnumSet<T> asEnumValues(Class<T> clazz, AnnotationValue a) {
    EnumSet<T> result = EnumSet.noneOf(clazz);
    asEnumValues(a).forEach(c -> result.add(Enum.valueOf(clazz, c.getSimpleName().toString())));
    return result;
  }

  /** Converts the given attribute to a type. */
  public static Optional<TypeMirror> asTypeValue(AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<TypeMirror, Void> {
      @Override
      public TypeMirror visitType(TypeMirror t, Void unused) {
        return t;
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
  }

  /** Converts the given annotation value to one or more strings. */
  public static Stream<String> asStrings(AnnotationValue v) {
    return MoreObjects.firstNonNull(
        v.accept(
            new SimpleAnnotationValueVisitor8<Stream<String>, Void>() {
              @Override
              public Stream<String> visitString(String s, Void unused) {
                return Stream.of(s);
              }

              @Override
              public Stream<String> visitArray(List<? extends AnnotationValue> list, Void unused) {
                return list.stream().flatMap(a -> a.accept(this, null)).filter(Objects::nonNull);
              }
            },
            null),
        Stream.empty());
  }

  /** Converts the given attribute to an annotation mirror. */
  public static Optional<AnnotationMirror> asAnnotationValue(AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<AnnotationMirror, Void> {
      @Override
      public AnnotationMirror visitAnnotation(AnnotationMirror a, Void unused) {
        return a;
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
  }

  /** Converts the given annotation value to one or more annotations. */
  public static Stream<AnnotationMirror> asAnnotations(AnnotationValue v) {
    return v.accept(
        new SimpleAnnotationValueVisitor8<Stream<AnnotationMirror>, Void>(Stream.empty()) {
          @Override
          public Stream<AnnotationMirror> visitAnnotation(AnnotationMirror av, Void unused) {
            return Stream.of(av);
          }

          @Override
          public Stream<AnnotationMirror> visitArray(
              List<? extends AnnotationValue> list, Void unused) {
            return list.stream().flatMap(a -> a.accept(this, null));
          }
        },
        null);
  }

  /** Converts the given annotation value to one or more types. */
  public static Stream<TypeMirror> asTypes(AnnotationValue v) {
    return v.accept(
        new SimpleAnnotationValueVisitor8<Stream<TypeMirror>, Void>(Stream.empty()) {
          @Override
          public Stream<TypeMirror> visitType(TypeMirror t, Void unused) {
            return Stream.of(t);
          }

          @Override
          public Stream<TypeMirror> visitArray(List<? extends AnnotationValue> list, Void unused) {
            return list.stream().flatMap(a -> a.accept(this, null));
          }
        },
        null);
  }

  /**
   * @deprecated Use {@link #getValue(AnnotationMirror, String)} instead.
   */
  @InlineMe(
      replacement = "MoreAnnotations.getValue(annotationMirror, name)",
      imports = "com.google.errorprone.util.MoreAnnotations")
  @Deprecated
  public static Optional<AnnotationValue> getAnnotationValue(
      AnnotationMirror annotationMirror, String name) {
    return getValue(annotationMirror, name);
  }

  /**
   * @deprecated Prefer {@link #getValue(AnnotationMirror, String)}.
   */
  @InlineMe(
      replacement = "MoreAnnotations.getValue((AnnotationMirror) attribute, name)",
      imports = {
        "com.google.errorprone.util.MoreAnnotations",
        "javax.lang.model.element.AnnotationMirror"
      })
  @Deprecated
  public static Optional<AnnotationValue> getAnnotationValue(
      Attribute.Compound attribute, String name) {
    return getValue((AnnotationMirror) attribute, name);
  }

  /** Returns the {@link TypeSymbol} representing the declaration of the annotation mirror. */
  public static TypeSymbol asElement(AnnotationMirror annotationMirror) {
    return (TypeSymbol) annotationMirror.getAnnotationType().asElement();
  }

  /**
   * Returns the set of {@link ElementType} targets specified in the annotation's {@code @Target}
   * meta-annotation, or {@code null} if no {@code @Target} meta-annotation is present.
   */
  public static @Nullable Set<ElementType> getTargetElementTypes(Element annotationSymbol) {
    if (annotationSymbol instanceof TypeSymbol typeSymbol) {
      var metadata = typeSymbol.getAnnotationTypeMetadata();
      if (metadata != null) {
        Compound target = metadata.getTarget();
        if (target != null) {
          return getTargetElementTypes(target);
        }
        return null;
      }
    }
    return getAnnotation(annotationSymbol, TARGET_ANNOTATION)
        .map(MoreAnnotations::getTargetElementTypes)
        .orElse(null);
  }

  private static Set<ElementType> getTargetElementTypes(AnnotationMirror target) {
    EnumSet<ElementType> result = EnumSet.noneOf(ElementType.class);
    for (VarSymbol element : getEnumValues(target, "value")) {
      try {
        result.add(ElementType.valueOf(element.getSimpleName().toString()));
      } catch (IllegalArgumentException e) {
        // Ignore unrecognized ElementType constants
      }
    }
    return result;
  }

  /**
   * Returns whether the annotation is a type annotation (i.e. its {@code @Target} includes {@link
   * ElementType#TYPE_USE} or {@link ElementType#TYPE_PARAMETER}).
   */
  public static boolean isTypeAnnotation(Element annotationSymbol) {
    Set<ElementType> targets = getTargetElementTypes(annotationSymbol);
    return targets != null
        && (targets.contains(ElementType.TYPE_USE) || targets.contains(ElementType.TYPE_PARAMETER));
  }

  /**
   * Returns the effective {@link RetentionPolicy} of the annotation symbol (defaulting to {@link
   * RetentionPolicy#CLASS} per JLS 9.6.4.2 if omitted or not found).
   */
  public static RetentionPolicy getRetentionPolicy(Element annotationSymbol) {
    return getEnumValue(annotationSymbol, RETENTION_ANNOTATION)
        .map(c -> asRetentionPolicy(c.getSimpleName().toString()))
        .orElse(RetentionPolicy.CLASS);
  }

  private static RetentionPolicy asRetentionPolicy(String name) {
    try {
      return RetentionPolicy.valueOf(name);
    } catch (IllegalArgumentException e) {
      return RetentionPolicy.CLASS;
    }
  }

  /**
   * Returns whether the annotation applies to the given {@link ElementType}. If the annotation has
   * no {@code @Target} meta-annotation, it is applicable in all declaration contexts (per JLS
   * 9.6.4.1), which includes all element types except {@link ElementType#TYPE_USE} and {@link
   * ElementType#TYPE_PARAMETER}.
   */
  public static boolean targetsElement(Element annotationSymbol, ElementType target) {
    Set<ElementType> targets = getTargetElementTypes(annotationSymbol);
    if (targets != null) {
      return targets.contains(target);
    }
    return target != ElementType.TYPE_USE && target != ElementType.TYPE_PARAMETER;
  }

  private MoreAnnotations() {}
}
