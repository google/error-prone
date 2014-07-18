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
public class ElementsCountedInLoopPositiveCases {
    
  public int testEnhancedFor(Iterable<Object> iterable, HashSet<Object> set, Object... array) {
    int count = 0;
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count ++;
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count += 1;
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count += 1.0; // float constant 1
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count += 1L; // long constant 1
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count  = count + 1;
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count  = 1 + count;
    }
    // BUG: Diagnostic contains: count += set.size()
    for (Object item : set) {
      count  = 1 + count;
    }
    // BUG: Diagnostic contains: count += array.length
    for (Object item : array) {
      count  = 1 + count;
    }
    return count;
  }
  
  public int testWhileLoop(List<Object> iterable) {
    Iterator<Object> it = iterable.iterator();
    int count = 0;
    // BUG: Diagnostic contains: 
    while (it.hasNext()) {
      count += 1;
    }
    // BUG: Diagnostic contains: 
    while (it.hasNext()) {
      count++;
    }
    // BUG: Diagnostic contains: 
    while (it.hasNext()) {
      count = count + 1;
    }
    return count;
  }
}
