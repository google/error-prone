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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is provided only to be overridden: it should not be
 * <i>invoked</i> from outside its declaring source file (as if it is {@code private}), and
 * overriding methods should not be directly invoked at all. Such a method represents a contract
 * between a class and its <i>subclasses</i> only, and is not to be considered part of the
 * <i>caller</i>-facing API of either class.
 *
 * <p>The annotated method must have protected or package-private visibility, and must not be {@code
 * static}, {@code final} or declared in a {@code final} class. Overriding methods must have either
 * protected or package-private visibility, although their effective visibility is actually "none".
 */
@Documented
@IncompatibleModifiers(
    modifier = {Modifier.PUBLIC, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL})
@Retention(CLASS) // Parent source might not be available while compiling subclass
@Target(METHOD)
public @interface ForOverride {}
