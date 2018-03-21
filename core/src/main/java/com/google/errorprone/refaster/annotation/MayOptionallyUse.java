/*
 * Copyright 2014 The Error Prone Authors.
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
 * Indicates that a parameter to a placeholder method is not required to be used in the
 * placeholder's implementation. For example, the pattern
 *
 * <pre><code>
 * abstract class Utf8Bytes {
 *  {@literal @}Placeholder abstract void handleException(
 *      {@literal @}MayOptionallyUse UnsupportedEncodingException e);
 *  {@literal @}Placeholder abstract void useString(String str);
 *  {@literal @}BeforeTemplate void before(byte[] array) {
 *     try {
 *       useString(new String(array, "UTF_8"));
 *     } catch (UnsupportedEncodingException e) {
 *       handleException(e);
 *     }
 *   }
 *  {@literal @}AfterTemplate void after(byte[] array) {
 *     useString(new String(array, StandardCharsets.UTF_8));
 *   }
 * }
 * </code></pre>
 *
 * would match even if the catch statement were empty, or didn't refer to e.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface MayOptionallyUse {}
