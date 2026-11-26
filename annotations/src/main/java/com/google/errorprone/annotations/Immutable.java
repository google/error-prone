/*
 * Copyright 2015 The Error Prone Authors.
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
package com.google.errorprone.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/// The class to which this annotation is applied is immutable.
///
/// An object is immutable if its state cannot be observed to change after construction. Immutable
/// objects are inherently thread-safe.
///
/// A class is immutable if all instances of that class are immutable. The immutability of a class
/// can only be fully guaranteed if the class is final, otherwise one must ensure all subclasses are
/// also immutable.
///
/// A conservative definition of object immutability is:
///
/// - All fields are final;
/// - All reference fields are of immutable type, or null;
/// - It is *properly constructed* (the `this` reference does not escape the constructor).
///
/// The requirement that all reference fields be immutable ensures *deep* immutability, meaning all
/// contained state is also immutable. A weaker property, common with container classes, is
/// *shallow* immutability, which allows some of the object's fields to point to mutable objects.
/// One example of shallow immutability is guava's ImmutableList, which may contain mutable
/// elements.
///
/// It is possible to implement immutable classes with some internal mutable state, as long as
/// callers can never observe changes to that state. For example, some state may be lazily
/// initialized to improve performance.
///
/// It is also technically possible to have an immutable object with non-final fields (see the
/// implementation of [String#hashCode()] for an example), but doing this correctly requires subtle
/// reasoning about safe data races and deep knowledge of the Java Memory Model.
///
/// Use of this annotation is validated by [Error Prone's immutability
/// analysis](https://errorprone.info/bugpattern/Immutable), which ensures that all
/// `@Immutable`-annotated classes are deeply immutable according to the conservative definition
/// above. Non-final classes may be annotated with `@Immutable`, and any code compiled by Error
/// Prone will be checked to ensure that no mutable subtypes of `@Immutable`-annotated classes
/// exist.
///
/// For more information about immutability, see:
///
/// - Java Concurrency in Practice §3.4
/// - Effective Java 3rd Edition §17
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Immutable {

  /// When annotating a generic type as immutable, `containerOf` specifies which type parameters
  /// must be instantiated with immutable types for the container to be deeply immutable.

  String[] containerOf() default {};
}
