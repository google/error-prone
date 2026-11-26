/*
 * Copyright 2023 The Error Prone Authors.
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

/// This annotation indicates that the class/interface it is applied to is thread safe.
///
/// An object is thread safe if no sequences of accesses (like reads and writes to public fields or
/// calls to public methods) may put the object into an invalid state, or cause it to violate its
/// contract, regardless of the interleaving of those actions at runtime.
///
/// This annotation has two related-but-distinct purposes:
///
/// - For humans: it indicates that the class/interface (and subclasses) is thread-safe
/// - For machines: it causes the annotated class/interface -- and all of its subtypes -- to be
///   validated by the `com.google.errorprone.bugpatterns.threadsafety.ThreadSafeChecker`
///   `BugChecker`.
///
/// Note that passing the checks performed by the `ThreadSafeChecker` is neither necessary nor
/// sufficient to guarantee the thread safety of a class. In fact, it is not possible to determine
/// thread safety through static code analysis alone, and the goal of `ThreadSafeChecker` is to
/// steer the code towards using standard thread-safe patterns, and then to assist the developer in
/// avoiding common mistakes. It is not meant as a substitute for diligent code review by a
/// knowledgeable developer.
///
/// Also note that the only easy way to guarantee thread-safety of a class is to make it immutable,
/// and you should do that whenever possible (or at the least, make as much of the class be
/// immutable). Otherwise, writing a thread-safe class is inherently tricky and error prone, and
/// keeping it thread-safe is even more so.
///
/// The remainder of this javadoc describes the heuristics enforced by `ThreadSafeChecker` and the
/// related `com.google.errorprone.bugpatterns.threadsafety.GuardedByChecker` and
/// `com.google.errorprone.bugpatterns.threadsafety.ImmutableChecker` on which the former relies.
///
/// The `ThreadSafeChecker` heuristics enforce that every field meets at least one of these
/// requirements:
///
/// - It is both `final` and its type is deemed inherently deeply thread-safe; and/or
/// - it is annotated with either [com.google.errorprone.annotations.concurrent.GuardedBy] (some
///   other annotations named `GuardedBy` also work, though this the preferred);
///
/// Below, more details about what is meant by "deemed inherently deeply thread-safe" are presented,
/// and, afterwards, more about `GuardedBy`.
///
/// A type is deemed inherently deeply thread-safe if it meets two requirements. The first
/// requirement is that it meets at least one of these four conditions:
///
/// - it is listed as a well-known immutable type in
///   `com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability` (e.g. a field of type
///   [String]); and/or
/// - it is listed as a well-known thread-safe type in
///   `com.google.errorprone.bugpatterns.threadsafety.WellKnownThreadSafety` (e.g. a field of type
///   [java.util.concurrent.atomic.AtomicBoolean]); and/or
/// - it is annotated with [Immutable]; and/or
/// - it is annotated with [ThreadSafe].
///
/// This first requirement means the type is at least inherently shallowly thread-safe. For types
/// with type parameters to be deemed deeply thread-safe, those of these types that denote
/// containment must also be deemed thread-safe. A full explanation of this can be found in the
/// `ThreadSafeTypeParameter` javadoc.
///
/// Fields annotated with `javax.annotation.concurrent.GuardedBy` are likely the meat of a mutable
/// thread-safe class: these are things that need to be mutated, but should be done so in a safe
/// manner -- i.e., (most likely) in critical sections of code that protect their access by means of
/// a lock. See more information in that annotation's javadoc.
///
/// As stated before, the heuristics above are not sufficient to guarantee the thread safety of a
/// class. Also as stated before, thread-safety is tricky, and requires diligent analysis by skilled
/// people. That said, we provide here a few examples of common examples of ways to break these
/// heuristics, so as to help you avoid them:
///
/// - a non-private `@GuardedBy` field -- i.e.if a non-private `@GuardedBy` field is accessed
///   outside the class, the code that enforces `@GuardedBy` will not prevent unprotected access
///   and/or modifications to the field;
/// - indirect access to the field. There are several ways in which code may access the objects
///   stored in the field indirectly (i.e. not directly referencing the field). In all these cases,
///   `@GuardedBy` offers no enforcement. Here's some examples:
///
///   - if the `@GuardedBy` field instance is part of an object by a method (e.g. a simple getter
///     method or constructor parameter);
///   - if a method takes an out-parameter and the method calls a method in that out-parameter
///     passing the instance of the `@GuardedBy` field, and that instance is stored in the
///     out-parameter (i.e. a simple setter method);
///
/// - methods that perform multiple operations -- e.g., if a class `Foo` contains a `AtomicInteger`
///   (which is an inherently deeply thread-safe data structure), the following code makes this
///   class not thread-safe:
///
///   ```
///   private void incrementMyAtomicInteger() {
///     myAtomicInteger.set(myAtomicInteger.get() + 1);
///   }
///   ```
///
/// Also see https://errorprone.info/bugpattern/ThreadSafe
// TODO(b/112275411): when fixed, delete the comment above about non-private fields
@Target({TYPE})
@Retention(RUNTIME)
// Note: besides abiding by the standard behavior of `@Inherited`, the behavior enforced by
// the static analysis effectively applies not only to classes that extend a class annotated with
// @ThreadSafe but *also* applies to classes that implement an annotated interface. This is because
// a class that implements an interface annotated with `@ThreadSafe` either: a. is anonymous, in
// which case the static analysis is implemented to run against it; or b. it is not anonymous, in
// which case the static analysis enforces that the class _also_ have an `@ThreadSafe` annotation.
@Inherited
@Documented
public @interface ThreadSafe {}
