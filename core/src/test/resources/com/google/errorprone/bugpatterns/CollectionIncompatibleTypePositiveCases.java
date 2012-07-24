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
    return collection.contains(this); //BUG
  }

  public boolean bug2() {
    return new ArrayList<String>().remove(new Date()); //BUG
  }

  public boolean bug3() {
    List<String> list = new ArrayList<String>(collection);
    return list.contains(new Exception()); //BUG
  }

  public String bug4() {
    Map<Integer, String> map = new HashMap<Integer, String>();
    return map.get("not an integer"); //BUG
  }
}
