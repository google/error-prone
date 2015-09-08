/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster.testdata;

import java.util.Random;

/**
 * Test data for {@code TwoLinesTemplate}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class TwoLinesToOneTemplateExample {
  public int example() {
    Random rng = new Random();
    int x = rng.nextInt();
    x = x + rng.nextInt();
    x = x + 20;
    x = x + 5;
    x = x + rng.nextInt(30);
    x = x + 20;
    // comments should block matching
    x = x + rng.nextInt();
    return x;
  }
}
