/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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


/** @author cushon@google.com (Liam Miller-Cushon) */
public class OverridesNegativeCase3 {
  abstract class Base {
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubOne extends Base {
    @Override
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubTwo extends SubOne {
    @Override
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubThree extends SubTwo {
    @Override
    abstract void arrayMethod(Object[] xs);
  }
}
