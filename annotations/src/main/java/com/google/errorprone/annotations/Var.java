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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The parameter or local variable to which this annotation is applied is non-final.
 *
 * <p>Most references are never modified, and accidentally modifying a reference is a potential
 * source of bugs. To prevent accidental modifications, the accompanying Error Prone <a
 * href="https://errorprone.info/bugpattern/Var">check</a> prevents parameters and local variables
 * from being modified unless they are explicitly annotated with @Var.
 *
 * <p>Since Java 8 can infer whether a local variable or parameter is effectively {@code final}, and
 * {@code @Var} makes it clear whether any variable is non- {@code final}, explicitly marking local
 * variables and parameters as {@code final} is discouraged.
 *
 * <p>The annotation can also be applied to fields, to indicate that the field is deliberately
 * non-final.
 */
@Target({FIELD, PARAMETER, LOCAL_VARIABLE})
@Retention(RUNTIME)
@IncompatibleModifiers(modifier = {Modifier.FINAL})
public @interface Var {}
