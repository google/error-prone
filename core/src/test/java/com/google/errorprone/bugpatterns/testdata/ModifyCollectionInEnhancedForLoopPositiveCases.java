/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * @author anishvisaria98@gmail.com (Anish Visaria)
 */
public class ModifyCollectionInEnhancedForLoopPositiveCases {

  public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
    for (Integer a : arr) {
      // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
      // thrown.
      arr.add(new Integer("42"));
      // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
      // thrown.
      arr.addAll(set);
      // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
      // thrown.
      arr.clear();
      // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
      // thrown.
      arr.remove(a);
      // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
      // thrown.
      arr.removeAll(set);
      // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
      // thrown.
      arr.retainAll(set);
    }
  }


  public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
    for (Integer x : arr) {
      for (Integer y : list) {
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        arr.add(y);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        arr.addAll(list);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        arr.clear();
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        arr.remove(x);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        arr.removeAll(list);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        arr.retainAll(list);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        list.add(x);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        list.addAll(arr);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        list.clear();
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        list.remove(y);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        list.removeAll(arr);
        // BUG: Diagnostic contains: This code will cause a ConcurrentModificationException to be
        // thrown.
        list.retainAll(arr);
      }
    }
  }

}


