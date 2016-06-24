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

package com.google.errorprone.bugpatterns.testdata;

/**
 * When a finally statement is exited because of a return, throw, break, or continue statement,
 * unintuitive behaviour can occur. Consider:
 * 
 * <pre>
 * {@code
 * finally foo() {
 *   try {
 *     return true;
 *   } finally {
 *     return false;
 *   }
 * }
 * </pre>
 * 
 * Because the finally block always executes, the first return statement has no effect and the
 * method will return false.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class FinallyPositiveCase1 {

  public static void test1() {
    while (true) {
      try {
      } finally {
        // BUG: Diagnostic contains: 
        break;
      }
    }
  }

  public static void test2() {
    while (true) {
      try {
      } finally {
        // BUG: Diagnostic contains: 
        continue;
      }
    }
  }

  public static void test3() {
    try {
    } finally {
      // BUG: Diagnostic contains: 
      return;
    }
  }

  public static void test4() throws Exception {
    try {
    } finally {
      // BUG: Diagnostic contains: 
      throw new Exception();
    }
  }
  
  /**
   * break statement jumps to outer labeled while, not inner one. 
   */
  public void test5() {
  label:
    while (true) {
      try {
      } finally {
        while (true) {
          // BUG: Diagnostic contains: 
          break label;
        }
      }
    }
  }
  
  /**
   * continue statement jumps to outer labeled for, not inner one. 
   */
  public void test6() {
  label:
    for (;;) {
      try {
      } finally {
        for (;;) {
          // BUG: Diagnostic contains: 
          continue label;
        }
      }
    }
  }
  
  /**
   * continue statement jumps to while, not switch. 
   */
  public void test7() {
    int i = 10;
    while (true) {
      try {
      } finally {
        switch (i) {
          case 10: 
            // BUG: Diagnostic contains: 
            continue;
        }
      }
    }
  }

  public void test8() {
    try {
    } finally {
    // BUG: Diagnostic contains: 
      { { { { { { { { { { return; } } } } } } } } } }
    }
  }

  // Don't assume that completion statements occur inside methods:
  static boolean flag = false;
  static {
    while (flag) {
      try {
      } finally {
        // BUG: Diagnostic contains: 
        break;
      }
    }
  }
}
