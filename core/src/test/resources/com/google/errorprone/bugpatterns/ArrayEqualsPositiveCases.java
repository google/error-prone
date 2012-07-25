/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import java.util.*;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayEqualsPositiveCases {
  public void intArray() {
    int[] a = {1, 2, 3};
    int[] b = {1, 2, 3};
    
    if (a.equals(b)) {  //BUG("Arrays.equals(a, b)")
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void objectArray() {
    Object[] a = new Object[3];
    Object[] b = new Object[3];
    
    if (a.equals(b)) {  //BUG("Arrays.equals(a, b)")
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void firstMethodCall() {
    String s = "hello";
    char[] b = new char[3];
    
    if (s.toCharArray().equals(b)) {  //BUG("Arrays.equals(s.toCharArray(), b)")
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void secondMethodCall() {
    char[] a = new char[3];
    String s = "hello";
    
    if (a.equals(s.toCharArray())) {  //BUG("Arrays.equals(a, s.toCharArray())")
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void bothMethodCalls() {
    String s1 = "hello";
    String s2 = "world";
    
    if (s1.toCharArray().equals(s2.toCharArray())) {  //BUG("Arrays.equals(s1.toCharArray(), s2.toCharArray())")
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
}
