/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.refaster.testdata;



import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Test input for {@code KeyBindingErrorTemplate}.
 */
class KeyBindingErrorTemplateExample {
    public int example(int x) {
      return x + 1;
    }
//  public ImmutableSet<Integer> example() {
//    Collection<Integer> iterable = new ArrayList<>();
//    iterable.add(1);
//    iterable.add(2);
//    iterable.add(3);
//
//    return iterable.stream().collect(toImmutableSet());
//  }
}
