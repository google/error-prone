/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import java.util.*;

/**
 * @author amshali@google.com (Amin Shali)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ElementsCountedInLoopNegativeCases {
  public int testEnhancedFor(List<Object> iterable) {
    int count = 0;
    // The following cases are considered negative because they are incrementing the counter by more 
    // than 1.
    for (Object item : iterable) {
      count += 2;
    }
    for (Object item : iterable) {
      count  = count + 3;
    }
    for (Object item : iterable) {
      count  = 2 + count;
    }
    return count;
  }

  public int testEnhancedWhileLoop(List<Object> iterable) {
    Iterator<Object> it = iterable.iterator();
    int count = 0;
    // The following case is considered negative because it is incrementing the counter by 2.
    while (it.hasNext()) {
      count += 2;
    }
    // 'this' is not an Iterable type.
    while (this.hasNext()) {
      count += 1;
    }
    // Complicated while body.
    while (it.hasNext()) {
      System.err.println("Not so simple body");
      count++;
    }
    return count;
  }

  public boolean hasNext() {
    return true;
  }
  
  public double testEnhancedForFloats(List<Object> iterable) {
    double count = 0;
    // The following cases are considered negative because they are incrementing the counter by a
    // float value which is not 1.
    for (Object item : iterable) {
      count += 2.0;
    }
    for (Object item : iterable) {
      count  = count + 3.0;
    }
    for (Object item : iterable) {
      count  = 0.1 + count;
    }
    return count;
  }
}
