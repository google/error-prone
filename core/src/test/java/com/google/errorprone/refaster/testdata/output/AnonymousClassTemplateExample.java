/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.errorprone.refaster.testdata;

import java.util.AbstractList;
import java.util.Collections;

/**
 * Test data for {@code AnonymousClassTemplate}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class AnonymousClassTemplateExample {
  public void sameOrderNoVariableConflicts() {
    System.out.println(Collections.nCopies(5, 17));
  }
  
  public void sameOrderVariableConflicts() {
    System.out.println(Collections.nCopies(5, 17));
  }

  public void differentOrderNoVariableConflicts() {
    System.out.println(Collections.nCopies(5, 17));
  }

  public void differentOrderVariableConflicts() {
    System.out.println(Collections.nCopies(5, 17));
  }

  public void fewerMethods() {
    System.out.println(new AbstractList<Integer>() {
      @Override
      public Integer get(int index) {
        return 17;
      }

      @Override
      public int size() {
        return 5;
      }
    });
  }

  public void moreMethods() {
    System.out.println(new AbstractList<Integer>() {
      @Override
      public Integer get(int index) {
        return 17;
      }

      @Override
      public Integer set(int index, Integer element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int size() {
        return 5;
      }

      @Override
      public void clear() {
        throw new UnsupportedOperationException();
      }
    });
  }
}
