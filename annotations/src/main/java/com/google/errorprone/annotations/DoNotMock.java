/*
 * Copyright 2016 The Error Prone Authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation representing a type that should not be mocked.
 *
 * <p>When marking a type {@code @DoNotMock}, you should always point to alternative testing
 * solutions such as standard fakes or other testing utilities.
 *
 * <p>Mockito tests can enforce this annotation by using a custom MockMaker which intercepts
 * creation of mocks.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface DoNotMock {
  /**
   * The reason why the annotated type should not be mocked.
   *
   * <p>This should suggest alternative APIs to use for testing objects of this type.
   */
  String value() default "Create a real instance instead";
}
