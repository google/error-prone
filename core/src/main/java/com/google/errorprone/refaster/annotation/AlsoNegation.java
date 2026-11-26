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

/// Indicates that Refaster should, additionally, refactor the negation of this rule and its
/// corresponding before templates. For example, given a `BeforeTemplate` with the code
/// `str.length() == 0` and an `@AfterTemplate @AlsoNegation` with the code `str.isEmpty()`,
/// Refaster would also rewrite `str.length() != 0` as `!str.isEmpty()`.
///
/// If this annotation is applied, all `BeforeTemplate` and `AfterTemplate` templates must be
/// expression templates with boolean return type.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@RequiredAnnotation(AfterTemplate.class)
public @interface AlsoNegation {}
