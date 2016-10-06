/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** @author yulissa@google.com (Yulissa Arroyo-Paredes) */
public class ArgumentParameterSwapPositiveCases {
  // names are identical but they are swapped
  public String doSomething(String[] keepPath, String[] dropPath, String topVal, String bottom) {
    return "wrong";
  }

  public void doSomethingElse() {
    int i = 0;
    String[] keepPath = {"one", "two", "three"};
    String[] dropPath = {"one"};
    String top = "top";
    String bottom = "bottom";
    // BUG: Diagnostic contains: 'doSomething(keepPath, dropPath, top, bottom);'
    doSomething(dropPath, keepPath, bottom, top);
  }

  // three parameters, all in the wrong place
  public String doNothing(String[] keepPath, String[] dropPath, String[] extraPath) {
    return "nothing";
  }

  public void doNothingElse() {
    String[] keepPath = {"one", "two", "three"};
    String[] dropPath = {"one"};
    String[] extraPath = {"extra"};
    // BUG: Diagnostic contains: 'doNothing(keepPath, dropPath, extraPath);'
    doNothing(extraPath, keepPath, dropPath);
  }

  // tests for when creating new objects
  class TestObject {
    final String sourceName;
    final String destinationName;

    TestObject(String sourceName, String destinationName) {
      super();
      this.sourceName = sourceName;
      this.destinationName = destinationName;
    }

    public TestObject resolveString() {
      // BUG: Diagnostic contains: 'return new TestObject(sourceName, destinationName);'
      return new TestObject(destinationName, sourceName);
    }
  }

  // test for calling super constructor
  class DerivedObject extends TestObject {
    DerivedObject(String sourceName, String destinationName, String other) {
      // BUG: Diagnostic contains: 'super(sourceName, destinationName);'
      super(destinationName, sourceName);
    }
  }

  // tests this(...) constructor
  static class ThisCall {
    ThisCall(String keepPath, String dropPath) {}

    ThisCall(String keepPath, String dropPath, String extraPath) {
      // BUG: Diagnostic contains: 'this(keepPath, dropPath);'
      this(dropPath, keepPath);
    }
  }
}
