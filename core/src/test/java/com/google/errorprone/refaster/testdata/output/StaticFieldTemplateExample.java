/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.errorprone.refaster.testdata;

import java.util.Collections;
import java.util.List;

/**
 * Test data for {@code StaticFieldTemplate}.
 *
 * @author mdempsky@google.com (Matthew Dempsky)
 */
public class StaticFieldTemplateExample {
  public void foo() {
    @SuppressWarnings("unchecked")
    List<Integer> list = Collections.emptyList();
    System.out.println(list);
  }
}
