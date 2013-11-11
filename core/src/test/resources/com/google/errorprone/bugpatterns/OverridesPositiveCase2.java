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

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * This tests that if there is a chain of method overrides where the varargs constraint is not
 * met, the suggested fixes make the entire chain of methods consistent with the root of the
 * override chain. So varargs -> array -> varargs -> array becomes all varargs.
 * 
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase2 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
  }

  abstract class SubOne extends Base {
    @Override
    //BUG: Suggestion includes "abstract void varargsMethod(Object... newNames);"
    abstract void varargsMethod(Object[] newNames);
  }
  
  abstract class SubTwo extends SubOne {
    @Override
    abstract void varargsMethod(Object... xs);
  }
  
  abstract class SubThree extends SubTwo {
    @Override
    //BUG: Suggestion includes "abstract void varargsMethod(Object... newNames);"
    abstract void varargsMethod(Object[] newNames);
  }
}