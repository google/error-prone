/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster.testdata.template;

import com.google.common.collect.Ordering;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;

/**
 * Example template calling {@code return} on a placeholder invocation.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public abstract class ReturnPlaceholderTemplate<T> {
  // This particular example is inspired by a much more practically useful example with Java 8...

  @Placeholder
  abstract int firstCompare(T left, T right);

  @Placeholder
  abstract int secondCompare(T left, T right);

  @BeforeTemplate
  Ordering<T> combined() {
    return new Ordering<T>() {
      @Override
      public int compare(T left, T right) {
        int cmp = firstCompare(left, right);
        if (cmp != 0) {
          return cmp;
        }
        return secondCompare(left, right);
      }
    };
  }

  @AfterTemplate
  Ordering<T> split() {
    return new Ordering<T>() {
      @Override
      public int compare(T left, T right) {
        /*
         * firstCompare was used as a one-line expression in @BeforeTemplate, but will be a return
         * placeholder here
         */
        return firstCompare(left, right);
      }
    }.compound(
        new Ordering<T>() {
          @Override
          public int compare(T left, T right) {
            return secondCompare(left, right);
          }
        });
  }
}
