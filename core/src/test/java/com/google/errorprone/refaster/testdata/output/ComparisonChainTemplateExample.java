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

import com.google.common.collect.ComparisonChain;


/**
 * Test data for {@code ComparisonChainTemplate}.
 */
public class ComparisonChainTemplateExample {
  public int compare(String a, String b) {
    return ComparisonChain.start().compare(Integer.valueOf(a.length()), Integer.valueOf(b.length())).compare(a, b).result();
    
  }
}
