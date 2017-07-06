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
public class OvershadowingSubclassFieldsPositiveCases1 {

  /** base class */
  public static class ClassA {
    protected String varOne;
    public int varTwo;
    String varThree;
  }


  /** ClassB has a variable name as parent */
  public static class ClassB extends ClassA {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    private String varOne = "Test";
  }

  /** ClassC has same variable name as grandparent */
  public static class ClassC extends ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    public int varTwo;
  }

  /** ClassD has same variable name as grandparent and other unrelated members */
  public static class ClassD extends ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    protected int varThree;
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    int varTwo;
    String randOne;
    String randTwo;
  }

  /** ClassE has same variable name as grandparent */
  public static class ClassE extends ClassC {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassC
    public String varTwo;
  }

  public static class ClassF extends ClassA {
    @SuppressWarnings("OvershadowingSubclassFields") // no warning because it's suppressed
    public String varThree;
  }

  public static class ClassG extends ClassF {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassF
    String varThree;
  }
}
