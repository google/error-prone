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

package com.google.errorprone.refaster.testdata.template;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/** */
public class KeyBindingErrorTemplate {
    @BeforeTemplate
    public int before(int a) {
      return a + 1;
    }

    @BeforeTemplate
    public int before(int a, int b) {
      b++;
      return a + 1;
    }

    @AfterTemplate
    public int after(int a, int b) {
      return a + b + 1;
    }
//  @BeforeTemplate
//  ImmutableSet<T> before(T[] iterable) {
//    return Refaster.anyOf(
//        ImmutableSet.<T>builder().add(iterable).build(),
//        Arrays.stream(iterable).collect(toImmutableSet()));
//  }
//
//  @BeforeTemplate
//  ImmutableSet<T> before(Iterator<T> iterable) {
//    return Refaster.anyOf(
//        ImmutableSet.<T>builder().addAll(iterable).build(),
//        Streams.stream(iterable).collect(toImmutableSet()));
//  }
//
//  @BeforeTemplate
//  ImmutableSet<T> before(Iterable<T> iterable) {
//    return Refaster.anyOf(
//        ImmutableSet.<T>builder().addAll(iterable).build(),
//        Streams.stream(iterable).collect(toImmutableSet()));
//  }
//
//  @BeforeTemplate
//  ImmutableSet<T> before(Collection<T> iterable) {
//    return iterable.stream().collect(toImmutableSet());
//  }
//
//  @AfterTemplate
//  ImmutableSet<T> after(Iterable<T> iterable) {
//    return ImmutableSet.copyOf(iterable);
//  }
}
