/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase5 {

  abstract class Base {
    abstract void varargsMethod(Object[] xs, Object... ys);
    abstract void arrayMethod(Object[] xs, Object[] ys);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: Did you mean 'abstract void arrayMethod(Object[] xs, Object[] ys);'
    abstract void arrayMethod(Object[] xs, Object... ys);

    @Override
    // BUG: Diagnostic contains: Did you mean 'abstract void varargsMethod(Object[] xs, Object... ys);'
    abstract void varargsMethod(Object[] xs, Object[] ys);

    void foo(Base base) {
      base.varargsMethod(null, new Object[] {}, new Object[] {}, new Object[] {}, new Object[] {});
    }
  }
}
