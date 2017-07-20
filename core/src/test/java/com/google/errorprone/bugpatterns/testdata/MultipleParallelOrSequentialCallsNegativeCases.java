/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
package com.google.errorprone.bugpatterns.testdata;

import java.util.List;

/** Created by mariasam on 7/6/17. */
public class MultipleParallelOrSequentialCallsNegativeCases {

  public void basicCase(List<String> list) {
    list.stream().parallel();
  }

  public void basicCaseSequential(List<String> list) {
    list.stream().sequential();
  }

  public void basicCaseNotLast(List<String> list) {
    list.stream().parallel().findFirst();
  }

  public void middleParallel(List<String> list) {
    list.stream().map(m -> m).parallel().filter(m -> m.isEmpty());
  }

  public void otherMethod() {
    SomeObject someObject = new SomeObject();
    someObject.parallel().parallel();
  }

  public void otherMethodNotParallel(List<String> list) {
    list.stream().filter(m -> m.isEmpty()).findFirst();
  }

  public void streamWithinAStreamImmediatelyAfter(List<String> list) {
    list.stream().map(m -> list.stream().parallel()).parallel();
  }

  public void streamWithinAStreamImmediatelyAfterOtherParallelBothFirstAndWithin(
      List<String> list) {
    list.stream().parallel().map(m -> list.stream().parallel());
  }

  public void streamWithinAStreamImmediatelyAfterOtherParallelBoth(List<String> list) {
    list.stream().sequential().map(m -> list.stream().parallel()).parallel();
  }

  class SomeObject {
    public SomeObject parallel() {
      return null;
    }
  }
}
