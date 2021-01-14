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

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify an error-prone {@link Matcher} to further restrict what expressions are matched by the
 * annotated parameter.
 *
 * <p><b>Note:</b> The {@code @Matches} annotation should <b>only</b> go on the
 * {@code @BeforeTemplate}. For example:
 *
 * <pre>{@code
 * class SingletonList {
 *   {@literal @}BeforeTemplate
 *   public <E> List<E> before({@literal @}Matches(IsNonNullMatcher.class) E e) {
 *     return Collections.singletonList(e);
 *   }
 *
 *   {@literal @}AfterTemplate
 *   public <E> List<E> after(E e) {
 *     return ImmutableList.of(e);
 *   }
 * }
 * }</pre>
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Matches {
  Class<? extends Matcher<? super ExpressionTree>> value();
}
