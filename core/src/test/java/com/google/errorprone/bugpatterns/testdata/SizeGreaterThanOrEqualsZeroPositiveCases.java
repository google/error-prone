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

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author glorioso@google.com (Nick Glorioso) */
public class SizeGreaterThanOrEqualsZeroPositiveCases {
  private List<Integer> intList = new ArrayList<>();
  private Set<Integer> intSet = new HashSet<>();
  private Map<Integer, Integer> intMap = new HashMap<>();
  private Collection<Integer> intCollection = intList;

  public boolean collectionSize() {

    // BUG: Diagnostic contains: !intList.isEmpty()
    boolean foo = intList.size() >= 0;

    // BUG: Diagnostic contains: !intSet.isEmpty()
    foo = intSet.size() >= 0;

    // BUG: Diagnostic contains: !intSet.isEmpty()
    foo = 0 <= intSet.size();

    // BUG: Diagnostic contains: !intMap.isEmpty()
    foo = intMap.size() >= 0;

    // BUG: Diagnostic contains: !intCollection.isEmpty()
    foo = intCollection.size() >= 0;

    // Yes, that works as java code
    // BUG: Diagnostic contains: !new ArrayList<Integer>().isEmpty()
    if (new ArrayList<Integer>().size() >= 0) {}

    CollectionContainer baz = new CollectionContainer();

    // BUG: Diagnostic contains: !baz.intList.isEmpty()
    if (baz.intList.size() >= 0) {}

    // BUG: Diagnostic contains: !baz.getIntList().isEmpty()
    if (baz.getIntList().size() >= 0) {}

    // BUG: Diagnostic contains: !Iterables.isEmpty(baz.getIntList())
    foo = Iterables.size(baz.getIntList()) >= 0;

    return foo;
  }

  public void stringLength() {
    String myString = "foo";
    CharSequence charSequence = myString;
    StringBuffer stringBuffer = new StringBuffer(myString);
    StringBuilder stringBuilder = new StringBuilder(myString);
    boolean foo = false;

    // BUG: Diagnostic contains: !myString.isEmpty()
    foo = myString.length() >= 0;

    // BUG: Diagnostic contains: !"My String Literal".isEmpty()
    foo = "My String Literal".length() >= 0;

    // BUG: Diagnostic contains: !myString.trim().substring(0).isEmpty();
    foo = myString.trim().substring(0).length() >= 0;

    // BUG: Diagnostic contains: charSequence.length() > 0
    foo = charSequence.length() >= 0;

    // BUG: Diagnostic contains: stringBuffer.length() > 0
    foo = stringBuffer.length() >= 0;

    // BUG: Diagnostic contains: 0 < stringBuffer.length()
    foo = 0 <= stringBuffer.length();

    // BUG: Diagnostic contains: stringBuilder.length() > 0
    foo = stringBuilder.length() >= 0;
  }

  private static int[] staticIntArray;
  private int[] intArray;
  private boolean[][] twoDarray;

  public boolean arrayLength() {

    // BUG: Diagnostic contains: intArray.length > 0
    boolean foo = intArray.length >= 0;

    // BUG: Diagnostic contains: twoDarray.length > 0
    foo = twoDarray.length >= 0;

    // BUG: Diagnostic contains: staticIntArray.length > 0
    foo = staticIntArray.length >= 0;

    // BUG: Diagnostic contains: twoDarray[0].length > 0
    foo = twoDarray[0].length >= 0;

    // BUG: Diagnostic contains: 0 < twoDarray[0].length
    foo = 0 <= twoDarray[0].length;

    // BUG: Diagnostic contains: (((((twoDarray))))).length > 0
    foo = (((((twoDarray))))).length >= 0;

    return foo;
  }

  private static class CollectionContainer {
    List<Integer> intList;

    List<Integer> getIntList() {
      return intList;
    }
  }
}
