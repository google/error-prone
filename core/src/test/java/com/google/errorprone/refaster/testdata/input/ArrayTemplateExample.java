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

import java.util.Arrays;
import java.util.List;

/**
 * Test data for {@code ArrayTemplate}.
 * 
 * @author mdempsky@google.com (Matthew Dempsky)
 */
public class ArrayTemplateExample {
  public void foo() {
    List<String> list = Arrays.asList("foo", "bar");
    System.out.println(list.get(1));
  }
}
