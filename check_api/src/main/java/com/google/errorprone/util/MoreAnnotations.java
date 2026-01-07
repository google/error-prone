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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Streams;
import com.sun.tools.javac.code.Attribute;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

/** Annotation-related utilities. */
public final class MoreAnnotations {

  /**
   * Returns the value of the annotation element-value pair with the given name if it is explicitly
   * set.
   */
  public static Optional<Attribute> getValue(Attribute.Compound attribute, String name) {
    return attribute.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().contentEquals(name))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  /**
   * Returns the value of the annotation element-value pair with the given name if it is explicitly
   * set.
   */
  public static Optional<AnnotationValue> getAnnotationValue(
      Attribute.Compound attribute, String name) {
    return getValue(attribute, name).map(a -> a);
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

  /** Converts the given attribute to an string value. */
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
  public static <T extends Enum<T>> Optional<T> asEnumValue(Class<T> clazz, AnnotationValue a) {
    class Visitor extends SimpleAnnotationValueVisitor8<T, Void> {

      @Override
      public T visitEnumConstant(VariableElement c, Void unused) {
        return Enum.valueOf(clazz, c.getSimpleName().toString());
      }
    }
    return Optional.ofNullable(a.accept(new Visitor(), null));
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
                return list.stream().flatMap(a -> a.accept(this, null)).filter(x -> x != null);
              }
            },
            null),
        Stream.empty());
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
    return asArray(v, MoreAnnotations::asTypeValue);
  }

  private static <T> Stream<T> asArray(
      AnnotationValue v, Function<AnnotationValue, Optional<T>> mapper) {
    class Visitor extends SimpleAnnotationValueVisitor8<Stream<T>, Void> {
      @Override
      public Stream<T> visitArray(List<? extends AnnotationValue> vals, Void unused) {
        return vals.stream().map(mapper).flatMap(Streams::stream);
      }
    }
    return MoreObjects.firstNonNull(v.accept(new Visitor(), null), Stream.of());
  }

  private MoreAnnotations() {}
}
