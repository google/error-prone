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

import com.google.common.collect.ComparisonChain;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;

/** Example use of placeholder methods in a {@code ComparisonChain} refactoring. */
public abstract class ComparisonChainTemplate<A extends Comparable<A>, B extends Comparable<B>> {
  @Placeholder(allowsIdentity = true)
  abstract <T> A propertyA(T t);

  @Placeholder(allowsIdentity = true)
  abstract <T> B propertyB(T t);

  @BeforeTemplate
  <T> int before(T left, T right) {
    int cmp = propertyA(left).compareTo(propertyA(right));
    if (cmp == 0) {
      return propertyB(left).compareTo(propertyB(right));
    } else {
      return cmp;
    }
  }

  @AfterTemplate
  <T> int after(T left, T right) {
    return ComparisonChain.start()
        .compare(propertyA(left), propertyA(right))
        .compare(propertyB(left), propertyB(right))
        .result();
  }
}
