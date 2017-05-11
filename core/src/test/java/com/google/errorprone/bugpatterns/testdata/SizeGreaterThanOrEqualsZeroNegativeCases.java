/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.testdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** @author glorioso@google.com (Nick Glorioso) */
public class SizeGreaterThanOrEqualsZeroNegativeCases {
  private List<Integer> intList = new ArrayList<>();
  private Set<Integer> intSet = new HashSet<>();
  private Collection<Integer> intCollection = intList;

  public boolean testEquality() {
    boolean foo;
    foo = intList.size() > 0;
    foo = intSet.size() >= 1;
    foo = intCollection.size() <= 0;
    foo = intCollection.size() == 0;
    foo = intCollection.size() < 0;

    if (new ArrayList<Integer>().size() > 0) {}

    CollectionContainer baz = new CollectionContainer();
    if (baz.intList.size() >= 1) {}
    if (baz.getIntList().size() >= 1) {}

    // These are incorrect comparisons, but we've chosen to not attempt to find these issues
    foo = (((((new HasASizeMethod()))))).size() >= 0;
    foo = new HasASizeMethod().length >= 0;

    return foo;
  }

  private static int[] staticIntArray;
  private int[] intArray;
  private boolean[][] twoDarray;

  public boolean arrayLength() {
    int zero = 0;

    boolean foo = intArray.length > 0;
    foo = twoDarray.length >= 1;
    foo = staticIntArray.length >= -1;
    foo = twoDarray[0].length > 0;
    foo = (((((twoDarray))))).length > zero;

    return foo;
  }

  private static class CollectionContainer {
    List<Integer> intList;

    List<Integer> getIntList() {
      return intList;
    }
  }

  private static class HasASizeMethod {
    public int length = 0;

    public int size() {
      return length;
    }
  }
}
