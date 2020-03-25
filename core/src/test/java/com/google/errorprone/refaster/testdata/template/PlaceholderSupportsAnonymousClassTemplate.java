/*
 * Copyright 2014 The Error Prone Authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executors;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;

/**
 * Test case demonstrating use of Refaster placeholder methods
 * to match anonymous classes.
 *
 * @author mketelaar@hubspot.com (Megan Ketelaar)
 */
public abstract class PlaceholderSupportsAnonymousClassTemplate<E> {
  @BeforeTemplate
  void before() {
    Iterators.getLast(someIterator());
  }

  // Contrived example

  @AfterTemplate
  void after() {
    Iterators.getLast(someIterator(), null);
  }

  @Placeholder
  abstract Iterator<E> someIterator();
}
