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

public class FallThroughPositiveCases {

  class NonTerminatingTryFinally {

    public int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            if (z > 0) {
              return i;
            } else {
              z++;
            }
          } finally {
            z++;
          }
          // BUG: Diagnostic contains:
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }

  abstract class TryWithNonTerminatingCatch {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            return bar();
          } catch (RuntimeException e) {
            log(e);
            throw e;
          } catch (Exception e) {
            log(e); // don't throw
          }
          // BUG: Diagnostic contains:
        case 1:
          return -1;
        default:
          return 0;
      }
    }

    abstract int bar() throws Exception;

    void log(Throwable e) {}
  }

  public class Tweeter {

    public int numTweets = 55000000;

    public int everyBodyIsDoingIt(int a, int b) {
      switch (a) {
        case 1:
          System.out.println("1");
          // BUG: Diagnostic contains:
        case 2:
          System.out.println("2");
          // BUG: Diagnostic contains:
        default:
      }
      return 0;
    }
  }
}
