/*
 * Copyright 2021 The Error Prone Authors.
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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element should not be removed, and that its visibility, modifiers,
 * type, and name should not be modified.
 *
 * <p>For example, program elements that are referenced via reflection may be annotated to prevent
 * them from being removed by static analysis that detects dead code.
 *
 * <p>If this annotates another annotation type, any member annotated with that annotation type
 * should also be kept.
 */
@Documented
@Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, TYPE})
@Retention(RUNTIME)
public @interface Keep {}
