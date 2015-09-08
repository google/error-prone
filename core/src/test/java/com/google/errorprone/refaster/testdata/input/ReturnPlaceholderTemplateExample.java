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

import com.google.common.collect.Ordering;

/**
 * Test data for {@code ReturnPlaceholderTemplate}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class ReturnPlaceholderTemplateExample {
  public static final Ordering<String> LENGTH_THEN_LOWER_CASE_ONE_LINE = new Ordering<String>() {
    @Override
    public int compare(String left, String right) {
      int lengthCmp = Integer.compare(left.length(), right.length());
      if (lengthCmp != 0) {
        return lengthCmp;
      }
      return left.toLowerCase().compareTo(right.toLowerCase());
    }
  };
  
  public static final Ordering<String> LENGTH_THEN_LOWER_CASE_MULTI_LINE = new Ordering<String>() {
    @Override
    public int compare(String left, String right) {
      int lengthCmp = Integer.compare(left.length(), right.length());
      if (lengthCmp != 0) {
        return lengthCmp;
      }
      String leftLower = left.toLowerCase();
      String rightLower = right.toLowerCase();
      return leftLower.compareTo(rightLower);
    }
  };
}
