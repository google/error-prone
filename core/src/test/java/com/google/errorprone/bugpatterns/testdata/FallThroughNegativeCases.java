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

import java.io.FileInputStream;
import java.io.IOException;

public class FallThroughNegativeCases {

  public class AllowAnyComment {

    public int numTweets = 55000000;

    public int everyBodyIsDoingIt(int a, int b) {
      switch (a) {
        case 1:
          System.out.println("1");
          // fall through
        case 2:
          System.out.println("2");
          break;
        default:
      }
      return 0;
    }
  }

  static class EmptyDefault {

    static void foo(String s) {
      switch (s) {
        case "a":
        case "b":
          throw new RuntimeException();
        default:
          // do nothing
      }
    }

    static void bar(String s) {
      switch (s) {
        default:
      }
    }
  }

  class TerminatedSynchronizedBlock {

    private final Object o = new Object();

    int foo(int i) {
      switch (i) {
        case 0:
          synchronized (o) {
            return i;
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }

  class TryWithNonTerminatingFinally {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            return i;
          } finally {
            z++;
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }

  abstract class TryWithTerminatingCatchBlocks {

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
            log(e);
            throw new RuntimeException(e);
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }

    int tryWithResources(String path, int i) {
      switch (i) {
        case 0:
          try (FileInputStream f = new FileInputStream(path)) {
            return f.read();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        case 1:
          try (FileInputStream f = new FileInputStream(path)) {
            return f.read();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        default:
          throw new RuntimeException("blah");
      }
    }

    abstract int bar() throws Exception;

    void log(Throwable e) {}
  }

  class TryWithTerminatingFinally {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            z++;
          } finally {
            return i;
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }
}
