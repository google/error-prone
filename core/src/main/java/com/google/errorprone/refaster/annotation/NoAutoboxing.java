/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a Refaster rule should not allow autoboxing or autounboxing when typechecking a
 * {@code @BeforeTemplate} match (its parameters, placeholder expressions, and return type against
 * the matched expression).
 *
 * <p>This does not prevent an {@code @AfterTemplate} from changing the expression's type between
 * boxed and primitive. For example, a rule whose {@code @BeforeTemplate} returns {@code Boolean}
 * (matching a {@code Boolean} expression without boxing or unboxing) and whose
 * {@code @AfterTemplate} returns {@code boolean} will still replace a boxed expression with a
 * primitive one.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@RequiredAnnotation(BeforeTemplate.class)
public @interface NoAutoboxing {}
