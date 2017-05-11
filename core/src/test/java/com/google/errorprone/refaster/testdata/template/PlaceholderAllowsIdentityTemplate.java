/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster.testdata.template;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;
import java.util.Collection;
import java.util.Iterator;

/**
 * Test case demonstrating use of Refaster placeholder methods.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public abstract class PlaceholderAllowsIdentityTemplate {
  @BeforeTemplate
  <E> void iteratorRemoveIf(Collection<E> collection) {
    Iterator<E> iterator = collection.iterator();
    while (iterator.hasNext()) {
      if (someBooleanCondition(iterator.next())) {
        iterator.remove();
      }
    }
  }

  @AfterTemplate
  <E> void iterablesRemoveIf(Collection<E> collection) {
    Iterables.removeIf(
        collection,
        new Predicate<E>() {
          @Override
          public boolean apply(E input) {
            return someBooleanCondition(input);
          }
        });
  }

  @Placeholder(allowsIdentity = true)
  abstract <E> boolean someBooleanCondition(E e);
}
