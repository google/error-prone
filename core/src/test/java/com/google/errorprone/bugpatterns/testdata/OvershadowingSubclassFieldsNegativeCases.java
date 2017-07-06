/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class OvershadowingSubclassFieldsNegativeCases {
  // base class
  static class ClassA {
    public int varOne;
  }

  // subclass with member variables of different names
  static class ClassB extends ClassA {
    private String varTwo;
    private int varThree;
    public static int varFour;
    public int varFive;
  }

  // subclass with initialized member variable of different name
  static class ClassC extends ClassB {
    private String varFour = "Test";

    // warning suppressed when overshadowing variable in parent
    @SuppressWarnings("OvershadowingSubclassFields")
    public int varFive;

    // warning suppressed when overshadowing variable in grandparent
    @SuppressWarnings("OvershadowingSubclassFields")
    public int varOne;
  }

  // subclass with member *methods* with the same name as superclass member variable -- this is ok
  static class ClassD extends ClassC {
    public void varThree() {}

    public void varTwo() {}
  }
}
