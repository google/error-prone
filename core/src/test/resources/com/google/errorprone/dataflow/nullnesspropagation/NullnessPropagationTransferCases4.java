/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;

/**
 * Tests for ==.
 */
public class NullnessPropagationTransferCases4 {

  public void equalBothNull() {
    String str1 = null;
    if (str1 == null) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);

    if (null == str1) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);

    String str2 = null;
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }
  
  public void equalBothNonNull() {
    String str1 = "foo";
    if (str1 == "bar") {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    
    if ("bar" == str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    
    String str2 = "bar";
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  }
  
  public void equalOneNullOtherNonNull() {
    String str1 = "foo";
    if (str1 == null) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    
    if (null == str1) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    
    String str2 = null;
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);

    if (str2 == str1) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }
  
  public void equalOneNullableOtherNull(String nullableParam) {
    String str1 = nullableParam;
    if (str1 == null) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    
    if (null == str1) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    
    String str2 = null;
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  
    if (str2 == str1) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }
  
  public void equalOneNullableOtherNonNull(String nullableParam) {
    String str1 = nullableParam;
    if (str1 == "foo") {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    
    if ("foo" == str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    
    String str2 = "foo";
    if (str1 == str2) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  
    if (str2 == str1) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    } else {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(str1);
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(str2);
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);
  }
  
  // TODO(user): tests for bottom?
}