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

/** @author eaftan@google.com (Eddie Aftandilian) */
public class FinallyNegativeCase1 {

  public static void test1() {
    while (true) {
      try {
        break;
      } finally {
      }
    }
  }

  public static void test2() {
    while (true) {
      try {
        continue;
      } finally {
      }
    }
  }

  public static void test3() {
    try {
      return;
    } finally {
    }
  }

  public static void test4() throws Exception {
    try {
      throw new Exception();
    } catch (Exception e) {
    } finally {
    }
  }

  /** break inner loop. */
  public void test5() {
    label:
    while (true) {
      try {
      } finally {
        while (true) {
          break;
        }
      }
    }
  }

  /** continue statement jumps out of inner for. */
  public void test6() {
    label:
    for (; ; ) {
      try {
      } finally {
        for (; ; ) {
          continue;
        }
      }
    }
  }

  /** break statement jumps out of switch. */
  public void test7() {
    int i = 10;
    while (true) {
      try {
      } finally {
        switch (i) {
          case 10:
            break;
        }
      }
    }
  }
}
