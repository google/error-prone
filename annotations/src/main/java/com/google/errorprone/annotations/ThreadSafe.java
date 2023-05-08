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

/**
 * This annotation indicates that the class/interface it is applied to is thread safe
 *
 * <p>An object is thread safe if no sequences of accesses (like reads and writes to public fields
 * or calls to public methods) may put the object into an invalid state, or cause it to violate its
 * contract, regardless of the interleaving of those actions at runtime.
 *
 * <p>This annotation has two related-but-distinct purposes:
 *
 * <ul>
 *   <li>For humans: it indicates that the class/interface (and subclasses) is thread-safe
 *   <li>For machines: it causes the annotated class/interface -- and all of its subtypes -- to be
 *       validated by the {@code com.google.errorprone.bugpatterns.threadsafety.ThreadSafeChecker}
 *       {@code BugChecker}.
 * </ul>
 *
 * Note that passing the checks performed by the {@code ThreadSafeChecker} is neither necessary nor
 * sufficient to guarantee the thread safety of a class. In fact, it is not possible to determine
 * thread safety through static code analysis alone, and the goal of {@code ThreadSafeChecker} is to
 * steer the code towards using standard thread-safe patterns, and then to assist the developer in
 * avoiding common mistakes. It is not meant as a substitute for diligent code review by a
 * knowledgeable developer.
 *
 * <p>Also note that the only easy way to guarantee thread-safety of a class is to make it
 * immutable, and you should do that whenever possible (or at the least, make as much of the class
 * be immutable). Otherwise, writing a thread-safe class is inherently tricky and error prone, and
 * keeping it thread-safe is even more so.
 *
 * <p>The remainder of this javadoc describes the heuristics enforced by {@code ThreadSafeChecker}
 * and the related {@code com.google.errorprone.bugpatterns.threadsafety.GuardedByChecker} and
 * {@code com.google.errorprone.bugpatterns.threadsafety.ImmutableChecker} on which the former
 * relies.
 *
 * <p>The {@code ThreadSafeChecker} heuristics enforce that every field meets at least one of these
 * requirements:
 *
 * <ul>
 *   <li>It is both {@code final} and its type is deemed inherently deeply thread-safe; and/or
 *   <li>it is annotated with either {@link com.google.errorprone.annotations.concurrent.GuardedBy}
 *       (some other annotations named {@code GuardedBy} also work, though this the preferred);
 * </ul>
 *
 * Below, more details about what is meant by "deemed inherently deeply thread-safe" are presented,
 * and, afterwards, more about {@code GuardedBy}.
 *
 * <p>A type is deemed inherently deeply thread-safe if it meets two requirements. The first
 * requirement is that it meets at least one of these four conditions:
 *
 * <ul>
 *   <li>it is listed as a well-known immutable type in {@code
 *       com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability} (e.g. a field of type
 *       {@link String}); and/or
 *   <li>it is listed as a well-known thread-safe type in {@code
 *       com.google.errorprone.bugpatterns.threadsafety.WellKnownThreadSafety} (e.g. a field of type
 *       {@link java.util.concurrent.atomic.AtomicBoolean}); and/or
 *   <li>it is annotated with {@link Immutable}; and/or
 *   <li>it is annotated with {@link ThreadSafe}.
 * </ul>
 *
 * <p>This first requirement means the type is at least inherently shallowly thread-safe.
 *
 * <p>Fields annotated with {@code javax.annotation.concurrent.GuardedBy} are likely the meat of a
 * mutable thread-safe class: these are things that need to be mutated, but should be done so in a
 * safe manner -- i.e., (most likely) in critical sections of code that protect their access by
 * means of a lock. See more information in that annotation's javadoc.
 *
 * <p>As stated before, the heuristics above are not sufficient to guarantee the thread safety of a
 * class. Also as stated before, thread-safety is tricky, and requires diligent analysis by skilled
 * people. That said, we provide here a few examples of common examples of ways to break these
 * heuristics, so as to help you avoid them:
 *
 * <ul>
 *   <li>a non-private {@code @GuardedBy} field -- i.e.if a non-private {@code @GuardedBy} field is
 *       accessed outside the class, the code that enforces {@code @GuardedBy} will not prevent
 *       unprotected access and/or modifications to the field;
 *   <li>indirect access to the field. There are several ways in which code may access the objects
 *       stored in the field indirectly (i.e. not directly referencing the field). In all these
 *       cases, {@code @GuardedBy} offers no enforcement. Here's some examples:
 *       <ul>
 *         <li>if the {@code @GuardedBy} field instance is part of an object by a method (e.g. a
 *             simple getter method or constructor parameter);
 *         <li>if a method takes an out-parameter and the method calls a method in that
 *             out-parameter passing the instance of the {@code @GuardedBy} field, and that instance
 *             is stored in the out-parameter (i.e. a simple setter method);
 *       </ul>
 *   <li>methods that perform multiple operations -- e.g., if a class {@code Foo} contains a {@code
 *       AtomicInteger} (which is an inherently deeply thread-safe data structure), the following
 *       code makes this class not thread-safe:
 *       <pre>{@code
 * private void incrementMyAtomicInteger() {
 *   myAtomicInteger.set(myAtomicInteger.get() + 1);
 * }
 *
 * }</pre>
 * </ul>
 *
 * Also see https://errorprone.info/bugpattern/ThreadSafe
 */
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
