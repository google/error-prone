/*
 * Copyright 2018 The Error Prone Authors.
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

import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/// When a [ThreadSafe] class has type parameters, annotating a parameter with
/// `ThreadSafeTypeParameter` enforces that declarations of this class must, for that type
/// parameter, use a type that is itself thread-safe.
///
/// Additionally, only type parameters that are annotated with `ThreadSafeTypeParameter` can be used
/// as field types that are not
/// [`@GuardedBy`][com.google.errorprone.annotations.concurrent.GuardedBy].
///
/// In more detail, consider this (valid) class:
///
/// ```
/// @ThreadSafe class MyThreadSafeClass<A, B, @ThreadSafeTypeParameter C> {
///
///   @GuardedBy("this") B b;
///
///   final C c;
///
///   MyThreadSafeClass(B b, C c) {
///     this.b = b;
///     this.c = c;
///   }
/// }
/// ```
///
/// Each of these three type parameters is valid for a different reason: type parameter `A` is ok
/// because it is simply not used as the type of a field; type parameter `B` is ok because it is
/// used as the type of a field that is declared to be `@GuardedBy`; finally, type parameter `C` is
/// ok because it is annotated with `ThreadSafeTypeParameter`. Furthermore, the declaration
/// `MyThreadSafeClass<Object, Object, String>` is valid, since the type parameter `C` (i.e.,
/// `String`) is thread-safe, whereas a declaration `MyThreadSafeClass<Object, Object, Object>`
/// would result in a compiler error.
///
/// Note: the `ThreadSafeTypeParameter` annotation has a secondary use case. If you annotate a type
/// parameter of a method, then callers to that method are only allowed to pass in a type that is
/// deemed thread-safe. For example, given the method declaration `static <@ThreadSafeTypeParameter
/// T> void foo(T foo) {}`, a call to `foo` must pass a parameter that is deemed thread-safe.
@Documented
@Target(TYPE_PARAMETER)
@Retention(RUNTIME)
public @interface ThreadSafeTypeParameter {}
