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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CollectionIncompatibleTypeNegativeCases {

  public boolean ok1() {
    return new ArrayList<String>().contains("ok");
  }

  class B extends Date {}

  public boolean ok2() {
    return new ArrayList<Date>().contains(new B());
  }

  public boolean ok3() {
    return new OtherCollection<String>().contains(new B());
  }

  public void ok4() {
    Map<Integer, String> map = new HashMap<Integer, String>();
    Object notAString = null;
    System.out.println(map.containsValue(notAString));

    System.out.println(map.containsKey(new Object()));
  }
  
  private class OtherCollection<E> {
    public boolean contains(Object o) {
      return true;
    }
  }
}
