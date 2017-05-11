/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.errorprone.annotations.concurrent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on any static or field that will be initialized lazily, where races yield no
 * semantic difference in the code. The canonical example of this is String.hashCode():
 *
 * <pre>{@code
 * public int hashCode() {
 *   int h = hash;
 *   if (h == 0 && value.length > 0) {
 *     char val[] = value;
 *
 *     for (int i = 0; i < value.length; i++) {
 *       h = 31 * h + val[i];
 *     }
 *     hash = h;
 *   }
 *   return h;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LazyInit {}
