/*
 * Copyright 2017 The Error Prone Authors.
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

/** @author anishvisaria98@gmail.com (Anish Visaria) */
public class ModifyCollectionInEnhancedForLoopPositiveCases {

  public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
    for (Integer a : arr) {
      // BUG: Diagnostic contains:
      arr.add(new Integer("42"));
      // BUG: Diagnostic contains:
      arr.addAll(set);
      // BUG: Diagnostic contains:
      arr.clear();
      // BUG: Diagnostic contains:
      arr.remove(a);
      // BUG: Diagnostic contains:
      arr.removeAll(set);
      // BUG: Diagnostic contains:
      arr.retainAll(set);
    }
  }

  public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
    for (Integer x : arr) {
      for (Integer y : list) {
        // BUG: Diagnostic contains:
        arr.add(y);
        // BUG: Diagnostic contains:
        arr.addAll(list);
        // BUG: Diagnostic contains:
        arr.clear();
        // BUG: Diagnostic contains:
        arr.remove(x);
        // BUG: Diagnostic contains:
        arr.removeAll(list);
        // BUG: Diagnostic contains:
        arr.retainAll(list);
        // BUG: Diagnostic contains:
        list.add(x);
        // BUG: Diagnostic contains:
        list.addAll(arr);
        // BUG: Diagnostic contains:
        list.clear();
        // BUG: Diagnostic contains:
        list.remove(y);
        // BUG: Diagnostic contains:
        list.removeAll(arr);
        // BUG: Diagnostic contains:
        list.retainAll(arr);
      }
    }
  }
}
