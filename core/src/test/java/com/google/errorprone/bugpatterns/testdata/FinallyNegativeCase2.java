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

import java.io.IOException;

/** @author cushon@google.com (Liam Miller-Cushon) */
public class FinallyNegativeCase2 {
  public void test1(boolean flag) {
    try {
      return;
    } finally {
    }
  }

  public void test2() throws Exception {
    try {
    } catch (Exception e) {
      throw new Exception();
    } finally {
    }
  }

  public void returnInAnonymousClass(boolean flag) {
    try {
    } finally {
      new Object() {
        void foo() {
          return;
        }
      };
    }
  }

  public void throwFromNestedTryInFinally() throws Exception {
    try {
    } finally {
      try {
        throw new Exception();
      } catch (Exception e) {
      } finally {
      }
    }
  }

  public void nestedTryInFinally2() throws Exception {
    try {
    } finally {
      try {
        // This exception will propogate out through the enclosing finally,
        // but we don't do exception analysis and have no way of knowing that.
        // Xlint:finally doesn't handle this either, since it only reports
        // situations where the end of a finally block is unreachable as
        // definied by JLS 14.21.
        throw new IOException();
      } catch (Exception e) {
      }
    }
  }
}
