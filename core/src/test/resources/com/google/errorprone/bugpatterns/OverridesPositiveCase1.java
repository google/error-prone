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
 * This tests that the a bug is reported when a method override changes the type of a parameter
 * from varargs to array, or array to varargs. It also ensures that the implementation can
 * handles cases with multiple parameters, and whitespaces between the square brackets for
 * array types.
 * 
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase1 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
    abstract void arrayMethod(int x, Object[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    //BUG: Suggestion includes "abstract void arrayMethod(int x, Object[] newNames);"
    abstract void arrayMethod(int x, Object... newNames);
  }
  
  abstract class Child2 extends Base {
    @Override
    //BUG: Suggestion includes "abstract void varargsMethod(Object... xs);"
    abstract void varargsMethod(Object[] xs);
  }
  
  abstract class Child3 extends Base {
    @Override
    //BUG: Suggestion includes "abstract void varargsMethod(Object... xs);"
    abstract void varargsMethod(Object[  ] xs);
  }

  abstract class Child4 extends Base {
    @Override
    //BUG: Suggestion includes "abstract void varargsMethod(Object... xs);"
    abstract void varargsMethod(Object[                           ] xs);
  }

  abstract class Child5 extends Base {
    @Override
    //BUG: Suggestion includes "Varargs"
    abstract void varargsMethod(Object[/**/                       ] xs);
  }
}