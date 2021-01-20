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

import java.util.Collection;

/** Sample data for InferLambdaBodyType. */
public class InferLambdaBodyTypeExample {
  static void example(Collection<Integer> collection) {
    collection.forEach((Integer i) -> System.out.println(i));
    collection.forEach((Integer i) -> { int j = i + 1; System.out.println(j); });
  }
}
