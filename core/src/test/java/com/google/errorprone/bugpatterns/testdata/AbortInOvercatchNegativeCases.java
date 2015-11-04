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
public class AbortInOvercatchNegativeCases {
  public void error() throws IllegalArgumentException {
	throw new IllegalArgumentException("Fake exception.");
  }
  
  public void abortInPreciseCatch() {
    int a = 0;
    try {
      error();
    } catch (IllegalArgumentException t) {
      System.exit(1); // This is not an overcatch, therefore the abort is safe.
    }
  }
  
  public void overcatchButNoAbort() {
    int a = 0;
    try {
      error();
    } catch (Exception e) {
      System.out.println("here...");
      a++; // This is an overcatch, but it does not abort, therefore it's OK.
    }
  }
}