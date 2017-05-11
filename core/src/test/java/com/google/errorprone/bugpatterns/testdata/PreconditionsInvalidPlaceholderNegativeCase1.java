/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;

public class PreconditionsInvalidPlaceholderNegativeCase1 {
  Integer foo;

  public void checkPositive(int x) {
    checkArgument(x > 0, "%s > 0", x);
  }

  public void checkTooFewArgs(int x) {
    checkArgument(x > 0, "%s %s", x);
  }

  public void checkFoo() {
    Preconditions.checkState(foo.intValue() == 0, "foo must be equal to 0 but was %s", foo);
  }

  public static void checkNotNull(Object foo, String bar, Object baz) {}

  public void checkSelf() {
    checkNotNull(foo, "Foo", this);
  }
}
