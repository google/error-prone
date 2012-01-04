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

package com.google.errorprone;

/**
 * An annotation intended for implementations of Matcher which is picked up by our 
 * documentation processor.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public @interface BugPattern {

  /**
   * Name should be unique
   */
  String name();
  
  Category category();
  
  public enum Category {
    JDK, GUAVA, APPLICATION_SPECIFIC
  }

  /**
   * Wiki syntax not allowed
   */
  String summary();
  
  /**
   * Wiki syntax allowed
   */
  String explanation();

  SeverityLevel severity();

  public enum SeverityLevel {
    WARNING, ERROR
  }

  MaturityLevel maturity();
  
  public enum MaturityLevel {
    ON_BY_DEFAULT,
    EXPERIMENTAL,
    PROPOSED,
  }
}
