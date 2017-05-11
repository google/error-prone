/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
import java.util.List;

/** @author scottjohnson@google.com (Scott Johnson) */
public class ModifyingCollectionWithItselfNegativeCases {

  List<Integer> a = new ArrayList<Integer>();

  public boolean addAll(List<Integer> b) {
    return a.addAll(b);
  }

  public boolean removeAll(List<Integer> b) {
    return a.removeAll(b);
  }

  public boolean retainAll(List<Integer> b) {
    return a.retainAll(b);
  }

  public boolean containsAll(List<Integer> b) {
    return a.containsAll(b);
  }
}
