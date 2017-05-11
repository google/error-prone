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

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Refaster template that should preserve types named in the original source.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class ExplicitTypesPreservedTemplate {
  @BeforeTemplate
  public <E> List<E> linkedList() {
    return new LinkedList<E>();
  }

  @AfterTemplate
  public <E> List<E> arrayList() {
    return new ArrayList<E>();
  }
}
