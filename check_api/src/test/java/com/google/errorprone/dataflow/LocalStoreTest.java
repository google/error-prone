/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow;

import static org.junit.Assert.assertEquals;

import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cpovirk@google.com (Chris Povirk) */
@RunWith(JUnit4.class)
public class LocalStoreTest {
  @Test
  public void leastUpperBoundEmpty() {
    assertEquals(newStore(), newStore().leastUpperBound(newStore()));
  }

  // TODO(cpovirk): more tests!

  private static LocalStore<Nullness> newStore() {
    return LocalStore.empty();
  }
}
