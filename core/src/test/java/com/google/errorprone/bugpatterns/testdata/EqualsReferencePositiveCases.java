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

/** Created by mariasam on 6/22/17. */
public class EqualsReferencePositiveCases {

  @Override
  // BUG: Diagnostic contains: ==
  public boolean equals(Object o) {
    System.out.println(this.equals(o));
    return true;
  }

  class EqualsInElse {
    @Override
    // BUG: Diagnostic contains: ==
    public boolean equals(Object o) {
      System.out.println(o == this);
      return this.equals(o);
    }
  }

  class FinalObject {
    @Override
    // BUG: Diagnostic contains: ==
    public boolean equals(final Object object) {
      return this.equals(object);
    }
  }

  class NoThis {
    @Override
    // BUG: Diagnostic contains: ==
    public boolean equals(Object o) {
      return equals(o);
    }
  }
}
