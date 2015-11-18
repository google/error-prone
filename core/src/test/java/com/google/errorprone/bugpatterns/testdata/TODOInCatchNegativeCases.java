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

/**
 * @author yuan@ece.toronto.edu (Ding Yuan)
 */
public class TODOInCatchNegativeCases {
  static final int FIXME = 0;

  public void error() throws IllegalArgumentException {
    throw new IllegalArgumentException("Fake exception.");
  }

  public void noTodoInCatch() {
    int a = 0;
    try {
      error();
    } catch (Throwable t) {
      // Handled!
      a++;
    }
  }
  
  public void todoNotInCatch() {
    int a = 0; // TODO
    try {
      error();
      // TODO
      /* FIXME */
    } catch (Throwable t) {
      // Handled!
      a++;
    }
  }
  
  public void todoAsSymbol() {
    int TODO = FIXME;
    int a;
    try {
      error();
    } catch (Throwable t) {
      // Handled!
      TODO++;
      a = FIXME + 1;
    }
  }
}
