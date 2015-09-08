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

import static java.util.Collections.emptyList;

import java.util.Collections;
import java.util.List;

/**
 * Test data for {@code ImplicitTypesInlinedTemplate}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class ImplicitTypesInlinedTemplateExample {
  @SuppressWarnings("unused")
  public void foo() {
    List<String> stringList = Collections.emptyList();
    List<Double> doubleList = emptyList();
    List<Integer> intList = Collections.synchronizedList(Collections.<Integer>emptyList());
  }
}
