/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CollectionIncompatibleTypePositiveCases {
  Collection<String> collection = new ArrayList<String>();

  public boolean bug() {
    // BUG: Diagnostic contains: return false
    return collection.contains(this);
  }

  public boolean bug2() {
    // BUG: Diagnostic contains: return false
    return new ArrayList<String>().remove(new Date());
  }

  public boolean bug3() {
    List<String> list = new ArrayList<String>(collection);
    // BUG: Diagnostic contains: false
    System.out.println(list.indexOf(new Integer(0)));
    // BUG: Diagnostic contains: false
    System.out.println(list.lastIndexOf(new Integer(0)));
    // BUG: Diagnostic contains: return false
    return list.contains(new Exception());
  }

  public String bug4() {
    Map<Integer, String> map = new HashMap<Integer, String>();
    // BUG: Diagnostic contains: false
    System.out.println(map.containsKey("not an integer"));

    Integer notAString = null;
    // BUG: Diagnostic contains: false
    System.out.println(map.containsValue(notAString));
    // BUG: Diagnostic contains: false
    System.out.println(map.remove("not an integer"));
    // BUG: Diagnostic contains: return false
    return map.get("not an integer");
  }
}
