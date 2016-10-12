/*
 * Copyright 2016 Google Inc. All rights reserved.
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

import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Repeated;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Template using {@code Refaster.asVarargs}.
 *
 * @author Louis Wasserman
 */
public class AsVarargsTemplate {
  @BeforeTemplate
  IntStream before(@Repeated IntStream streams) {
    return Stream.of(Refaster.asVarargs(streams)).flatMap(s -> s.boxed()).mapToInt(i -> i);
  }

  @AfterTemplate
  IntStream after(@Repeated IntStream streams) {
    return Stream.of(streams).flatMapToInt((IntStream s) -> s);
  }
}
