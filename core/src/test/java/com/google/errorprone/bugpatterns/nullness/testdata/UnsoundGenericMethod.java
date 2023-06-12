/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness.testdata;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class UnsoundGenericMethod {
  public interface Marker {}

  public interface Converter<T extends Marker> {
    List<?> convert(T input);
  }

  // error below can be avoided here with "class Impl<T extends Marker> ..."
  private static class Impl<T> implements Function<T, List<?>> {
    private final Stream<Converter<? super T>> cs;

    private Impl(Stream<Converter<? super T>> cs) {
      this.cs = cs;
    }

    @Override
    public List<?> apply(T input) {
      // BUG: Diagnostic contains: Unsafe wildcard in inferred type argument
      return cs.map(c -> new Wrap<>(c).handle(input)).collect(toList());
    }
  }

  private static class Wrap<T extends Marker> {
    Wrap(Converter<? super T> unused) {}

    T handle(T input) {
      return input;
    }
  }

  public static void main(String... args) {
    // BUG: Diagnostic contains: impossible
    new Impl<>(Stream.of(null, null)).apply("boom");
  }
}
