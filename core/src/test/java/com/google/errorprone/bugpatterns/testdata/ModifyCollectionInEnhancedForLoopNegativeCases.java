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
public class ModifyCollectionInEnhancedForLoopNegativeCases {

  public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
    for (Integer a : arr) {
      set.add(a);
      set.addAll(arr);
      set.clear();
      set.removeAll(arr);
      set.retainAll(arr);
    }

    for (Integer i : set) {
      arr.add(i);
      arr.addAll(set);
      arr.clear();
      arr.removeAll(set);
      arr.retainAll(set);
    }
  }

  public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
    for (Integer x : arr) {
      for (Integer y : list) {

      }
      list.add(x);
      list.addAll(arr);
      list.clear();
      list.removeAll(arr);
      list.retainAll(arr);
    }
  }

}
