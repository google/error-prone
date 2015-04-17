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

package com.google.errorprone.suppress;

/**
 * Test cases to ensure SuppressWarnings annotation is respected.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@SuppressWarnings("DeadException")
public class SuppressWarningsNegativeCases {

  @SuppressWarnings({"EmptyIf", "EmptyStatement"})
  public void testEmptyIf() {
    int i = 0;
    if (i == 10); {
      System.out.println("foo");
    }
  }

  @SuppressWarnings({"bar", "SelfAssignment"})
  public void testSelfAssignment() {
    int i = 0;
    i = i;
  }

  public void testDeadException() {
    new RuntimeException("whoops");
  }
}
