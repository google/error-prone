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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the return value of the annotated method must be checked. An error is triggered
 * when one of these methods is called but the result is not used.
 *
 * <p>{@code @CheckReturnValue} may be applied to a class or package to indicate that all methods in
 * that class or package must have their return values checked. For convenience, we provide an
 * annotation, {@link CanIgnoreReturnValue}, to exempt specific methods or classes from this
 * behavior.
 */
@Documented
@Target({METHOD, CONSTRUCTOR, TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface CheckReturnValue {}
