/*
 * Copyright 2017 The Error Prone Authors.
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
package com.google.errorprone.apply;

import com.google.common.collect.ImmutableList;

/** The different ways to order static imports. */
enum StaticOrder {
  /**
   * Sorts import statements so that all static imports come before all non-static imports and
   * otherwise sorted alphabetically.
   */
  STATIC_FIRST(ImmutableList.of(Boolean.TRUE, Boolean.FALSE)),

  /**
   * Sorts import statements so that all static imports come after all non-static imports and
   * otherwise sorted alphabetically.
   */
  STATIC_LAST(ImmutableList.of(Boolean.FALSE, Boolean.TRUE));

  private final ImmutableList<Boolean> groupOrder;

  StaticOrder(ImmutableList<Boolean> groupOrder) {
    this.groupOrder = groupOrder;
  }

  public Iterable<Boolean> groupOrder() {
    return groupOrder;
  }
}
