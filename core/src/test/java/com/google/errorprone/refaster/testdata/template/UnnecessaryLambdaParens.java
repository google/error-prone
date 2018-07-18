/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnnecessaryLambdaParen<T> {
  @BeforeTemplate
  Optional<T> last(Stream<T> stream) {
    return stream.map((x) -> x).collect(Collectors.reducing((a, b) -> b));
  }

  @AfterTemplate
  Optional<T> reduce(Stream<T> stream) {
    return stream.map((x) -> x).reduce((a,b) -> b);
  }
}
