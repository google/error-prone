/*
 * Copyright 2017 The Error Prone Authors.
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

/// Indicates that the annotated method should not be called under any normal circumstances, yet is
/// either *impossible* to remove, or *should not* ever be removed. Example:
///
/// ```
/// public class ImmutableList<E> implements List<E> {
///   @DoNotCall("guaranteed to throw an exception")
///   @Override public add(E e) {
///     throw new UnsupportedOperationException();
///   }
/// }
/// ```
///
/// By the demands of the `List` interface, this method can never be removed. However, since it
/// should always throw an exception, there can be no valid reason to call it except in the rarest
/// of circumstances. Although this information can only benefit users who have a reference of type
/// `ImmutableList` (not [List]), it's a good start.
///
/// If the typical caller's best remedy is to "inline" the method, [InlineMe] is probably a better
/// option; read there for more information. Using both annotations together is probably
/// unnecessary.
///
/// **Note on testing:** A `@DoNotCall` method should still have unit tests.
///
/// # `@DoNotCall` and deprecation
///
/// Deprecation may feel inappropriate or misleading in cases where there is no intention to ever
/// remove the method. It might create the impression of a "blemish" on your API; something that
/// should be fixed. Moreover, it's generally hard to enforce deprecation warnings as strongly as a
/// `@DoNotCall` violation should be.
///
/// But, when choosing the `@DoNotCall` option, consider adding [Deprecated] as well anyway, so that
/// any tools that don't support `@DoNotCall` can still do something reasonable to discourage usage.
/// This practice does have some cost; for example, suppression would require
/// `@SuppressWarnings({"DoNotCall", "deprecation"})`.
///
/// # Tool support
///
/// Error Prone supports this annotation via its
/// [DoNotCall](https://errorprone.info/bugpattern/DoNotCall) pattern.
@Retention(CLASS)
@Target(METHOD)
public @interface DoNotCall {

  /// An optional explanation of why the method should not be called.

  String value() default "";
}
