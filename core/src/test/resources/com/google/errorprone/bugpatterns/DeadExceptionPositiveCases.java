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

public class DeadExceptionPositiveCases {
  public void error() {
    //BUG: Suggestion includes "throw new RuntimeException"
    new RuntimeException("Not thrown, and reference lost");
  }

  public void fixIsToDeleteTheFirstStatement() {
    //BUG: Suggestion includes "remove this line"
    new IllegalArgumentException("why is this here?");
    int i = 1;
    System.out.println("i = " + i);
  }

  public void firstStatementWithNoSurroundingBlock() {
    if (true)
      //BUG: Suggestion includes "throw new InterruptedException"
      new InterruptedException("this should be thrown");

    if (true)
      return;
    else
      //BUG: Suggestion includes "throw new ArithmeticException"
      new ArithmeticException("should also be thrown");
  }
}