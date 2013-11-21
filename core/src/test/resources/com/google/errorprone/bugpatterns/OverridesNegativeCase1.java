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

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesNegativeCase1 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
    abstract void arrayMethod(Object[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    abstract void varargsMethod(final Object... newNames);
  }
  
  abstract class Child2 extends Base {
    @Override
    abstract void arrayMethod(Object[] xs);
  }
  
  static class StaticClass {
    static void staticVarargsMethod(Object... xs) {
    }
    
    static void staticArrayMethod(Object[] xs) { 
    }
  }

  interface Interface {
    void varargsMethod(Object... xs);
    void arrayMethod(Object[] xs);
  }
  
  abstract class ImplementsInterface implements Interface {
    public abstract void varargsMethod(Object... xs);
    public abstract void arrayMethod(Object[] xs);
  }
}