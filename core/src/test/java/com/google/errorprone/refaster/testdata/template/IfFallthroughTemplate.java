/*
 * Copyright 2015 Google Inc. All rights reserved.
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

/** Test template to make sure if fallthrough works. */
public class IfFallthroughTemplate<T> {
  @BeforeTemplate
  public int before(T left, T right, Ordering<? super T> ordering) {
    if (left == null && right == null) {
      return 0;
    } else if (left == null) {
      return -1;
    } else if (right == null) {
      return 1;
    } else {
      return ordering.compare(left, right);
    }
  }

  @AfterTemplate
  int after(T left, T right, Ordering<? super T> ordering) {
    return ordering.nullsFirst().compare(left, right);
  }
}
