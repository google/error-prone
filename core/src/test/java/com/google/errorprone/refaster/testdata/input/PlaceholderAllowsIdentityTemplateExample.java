/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster.testdata;

import java.util.Iterator;
import java.util.List;

/**
 * Test data for {@code PlaceholderTemplate}.
 */
public class PlaceholderAllowsIdentityTemplateExample {
  public void positiveExample(List<Integer> list) {
    Iterator<Integer> itr = list.iterator();
    while (itr.hasNext()) {
      if (itr.next() < 0) {
        itr.remove();
      }
    }
  }
  
  public void positiveIdentityExample(List<Boolean> list) {
    Iterator<Boolean> itr = list.iterator();
    while (itr.hasNext()) {
      if (itr.next()) {
        itr.remove();
      }
    }
  }

  public void refersToForbiddenVariable(List<Integer> list) {
    Iterator<Integer> itr = list.iterator();
    while (itr.hasNext()) {
      if (itr.next() < list.size()) {
        itr.remove();
      }
    }
  }
}
