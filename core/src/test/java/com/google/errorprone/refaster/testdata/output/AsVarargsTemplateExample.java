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

package com.google.errorprone.refaster.testdata;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Test data for AsVarargsTemplate.
 * 
 * @author Louis Wasserman
 */
public class AsVarargsTemplateExample {
  public void example() {
    System.out.println(
        Stream.of(IntStream.of(1), IntStream.of(2)).flatMapToInt((IntStream s) -> s).sum());
    
    // unchanged, it's not using the varargs overload
    System.out.println(
        Stream.of(IntStream.of(1)).flatMap(s -> s.boxed()).mapToInt(i -> i).sum());
  }
}
