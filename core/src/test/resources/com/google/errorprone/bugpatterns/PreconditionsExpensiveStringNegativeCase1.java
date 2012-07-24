/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import com.google.common.base.Preconditions;

/**
 * Preconditions calls which shouldn't be picked up for expensive string operations
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringNegativeCase1 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo %s foo  is not a good foo", foo);

    // This call should not be converted because of the %d, which does some locale specific
    // behaviour. If it were an %s, it would be fair game.
    Preconditions.checkState(true, String.format("The foo %d foo is not a good foo", foo));
  }
}
