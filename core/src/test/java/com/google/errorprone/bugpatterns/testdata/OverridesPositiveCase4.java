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
 * Test that the suggested fix is correct in the presence of whitespace, comments.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase4 {

  @interface Note { }

  abstract class Base {
    abstract void varargsMethod(@Note final Map<Object, Object>... xs);
    abstract void arrayMethod(@Note final Map<Object, Object>[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: (@Note final Map<Object, Object> /* asd */ [] /* dsa */ xs);
    abstract void arrayMethod(@Note final Map<Object, Object> /* asd */ ... /* dsa */ xs);
  }

  abstract class Child2 extends Base {
    @Override
    //TODO(cushon): improve testing infrastructure so we can enforce that no fix is suggested.
    // BUG: Diagnostic contains: Varargs
    abstract void varargsMethod(@Note final Map<Object, Object>  /*dsa*/ [ /* [ */ ] /* dsa */ xs);
  }
}