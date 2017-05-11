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

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

public class PreconditionsCheckNotNullPrimitivePositiveCases {

  private Tester field = new Tester();

  public void test() {
    Object a = new Object();
    Object b = new Object();
    byte byte1 = 0;
    short short1 = 0;
    int int1 = 0, int2 = 0;
    long long1 = 0;
    float float1 = 0;
    double double1 = 0;
    boolean boolean1 = false, boolean2 = false;
    char char1 = 0;
    Tester tester = new Tester();

    // Do we detect all primitive types?

    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(byte1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(short1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(int1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(long1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(float1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(double1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(boolean1);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(char1);

    // Do we give the right suggested fix?

    // BUG: Diagnostic contains: boolean1 = boolean2;
    boolean1 = Preconditions.checkNotNull(boolean2);
    // BUG: Diagnostic contains: boolean1 = int1 == int2;
    boolean1 = Preconditions.checkNotNull(int1 == int2);
    // BUG: Diagnostic contains: checkState(tester.hasId())
    Preconditions.checkNotNull(tester.hasId());
    // BUG: Diagnostic contains: checkState(tester.hasId(), "Must have ID!")
    Preconditions.checkNotNull(tester.hasId(), "Must have ID!");
    // BUG: Diagnostic contains: checkState(tester.hasId(), "Must have %s!", "ID")
    Preconditions.checkNotNull(tester.hasId(), "Must have %s!", "ID");

    // Do we handle arguments that evaluate to a primitive type?

    // BUG: Diagnostic contains: Preconditions.checkNotNull(a)
    Preconditions.checkNotNull(a != null);
    // BUG: Diagnostic contains: Preconditions.checkNotNull(a)
    Preconditions.checkNotNull(a == null);
    // BUG: Diagnostic contains: Preconditions.checkState(int1 == int2)
    Preconditions.checkNotNull(int1 == int2);
    // BUG: Diagnostic contains: Preconditions.checkState(int1 > int2)
    Preconditions.checkNotNull(int1 > int2);
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull(boolean1 ? int1 : int2);

    // Do we handle static imports?

    // BUG: Diagnostic contains: remove this line
    checkNotNull(byte1);
    // BUG: Diagnostic contains: 'checkState(tester.hasId())
    checkNotNull(tester.hasId());
  }

  public void test2(Tester arg) {
    Tester local = new Tester();
    // Do we correctly distinguish checkArgument from checkState?

    // BUG: Diagnostic contains: checkArgument(arg.hasId())
    checkNotNull(arg.hasId());
    // BUG: Diagnostic contains: checkState(field.hasId())
    checkNotNull(field.hasId());
    // BUG: Diagnostic contains: checkState(local.hasId())
    checkNotNull(local.hasId());
    // BUG: Diagnostic contains: checkState(!local.hasId())
    checkNotNull(!local.hasId());

    // BUG: Diagnostic contains: checkArgument(!(arg instanceof Tester))
    checkNotNull(!(arg instanceof Tester));

    // BUG: Diagnostic contains: checkState(getTrue())
    checkNotNull(getTrue());

    // BUG: Diagnostic contains: remove this line
    checkNotNull(arg.getId());
    // BUG: Diagnostic contains: id = arg.getId()
    int id = checkNotNull(arg.getId());

    // BUG: Diagnostic contains: boolean b = arg.hasId();
    boolean b = checkNotNull(arg.hasId());

    // Do we handle long chains of method calls?

    // BUG: Diagnostic contains: checkArgument(arg.getTester().getTester().hasId())
    checkNotNull(arg.getTester().getTester().hasId());

    // BUG: Diagnostic contains: checkArgument(arg.tester.getTester().hasId())
    checkNotNull(arg.tester.getTester().hasId());
  }

  private boolean getTrue() {
    return true;
  }

  private static class Tester {
    public Tester tester;

    public boolean hasId() {
      return true;
    }

    public int getId() {
      return 10;
    }

    public Tester getTester() {
      return tester;
    }
  }
}
