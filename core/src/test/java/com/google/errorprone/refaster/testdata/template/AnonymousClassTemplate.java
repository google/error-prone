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

package com.google.errorprone.refaster.testdata.template;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.OfKind;
import com.sun.source.tree.Tree.Kind;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Example template using an anonymous class, demonstrating that methods may be matched in any
 * order.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class AnonymousClassTemplate {
  @BeforeTemplate
  List<Integer> anonymousAbstractList(@OfKind(Kind.INT_LITERAL) final int value, final int size) {
    return new AbstractList<Integer>() {

      @Override
      public Integer get(int index) {
        return value;
      }

      @Override
      public int size() {
        return size;
      }

      @Override
      public Integer set(int index, Integer element) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @AfterTemplate
  List<Integer> nCopies(final int value, final int size) {
    return Collections.nCopies(size, value);
  }
}
