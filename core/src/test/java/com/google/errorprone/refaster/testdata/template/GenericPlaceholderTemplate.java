/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import com.google.common.base.Joiner;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;
import java.util.ArrayList;
import java.util.List;

/** Example template using a placeholder with generics to be inferred. */
public abstract class GenericPlaceholderTemplate<E> {
  @Placeholder
  abstract E generate();

  @BeforeTemplate
  void before(int n) {
    for (int i = 0; i < n; i++) {
      System.out.println(generate()); // E can only be inferred by looking at the actual type here
    }
  }

  @AfterTemplate
  void after(int n) {
    List<E> list = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      list.add(generate());
    }
    System.out.println(Joiner.on('\n').join(list));
  }
}
