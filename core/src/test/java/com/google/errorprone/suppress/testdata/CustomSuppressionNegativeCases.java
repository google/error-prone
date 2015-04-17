/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.suppress;

/**
 * Test cases to ensure that checks that use custom suppression annotations are suppressible via 
 * the custom suppression annotation.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CustomSuppressionNegativeCases {
  @CustomSuppressionTest.SuppressMyChecker
  public void testMyChecker() {
    return;
  }
  
  @CustomSuppressionTest.SuppressMyChecker
  @CustomSuppressionTest.SuppressMyChecker2
  public void testMultipleCustomSuppressions() {
    ;
    return;
  }
  
  public interface SimpleInterface {
    public void foo();
  }
  
  public void testEnclosingScope() {
    @CustomSuppressionTest.SuppressMyChecker
    SimpleInterface mySimpleInterface = new SimpleInterface() {
      public void foo() {
        return;
      }
    };
  }
}
