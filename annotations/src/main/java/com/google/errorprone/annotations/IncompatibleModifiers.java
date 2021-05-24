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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.lang.model.element.Modifier;

/**
 * Annotation declaring that the target annotation is incompatible with any one of the provided
 * modifiers. For example, an annotation declared as:
 *
 * <pre>
 * {@literal @}IncompatibleModifiers(Modifier.PUBLIC)
 * {@literal @}interface MyAnnotation {}
 * </pre>
 *
 * <p>will be considered illegal when used as:
 *
 * <pre>
 * {@literal @}MyAnnotation public void foo() {}
 * </pre>
 *
 * @author benyu@google.com (Jige Yu)
 */
@Documented
@Retention(RetentionPolicy.CLASS) // Element's source might not be available during analysis
@Target(ElementType.ANNOTATION_TYPE)
public @interface IncompatibleModifiers {
  /**
   * The incompatible modifiers. The annotated element is illegal with the presence of any one or
   * more of these modifiers.
   *
   * <p>Empty array has the same effect as not applying this annotation at all; duplicates are
   * allowed but have no effect.
   */
  Modifier[] value();
}
