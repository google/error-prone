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

import com.google.common.base.Joiner;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

/**
 * Example Refaster template using labeled statements, {@code break}, and {@code continue}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class LabelTemplate {
  @BeforeTemplate
  void join1(StringBuilder builder, Object[] elements) {
    for (int i = 0; i < elements.length; i++) {
      builder.append(elements[i]);
      if (i == elements.length - 1) {
        break;
      }
      builder.append(',');
    }
  }

  @BeforeTemplate
  void join2(StringBuilder builder, Object[] elements) {
    loop:
    for (int i = 0; i < elements.length; i++) {
      builder.append(elements[i]);
      if (i == elements.length - 1) {
        break loop;
      }
      builder.append(',');
    }
  }

  @BeforeTemplate
  void join3(StringBuilder builder, Object[] elements) {
    loop:
    for (int i = 0; i < elements.length; i++) {
      builder.append(elements[i]);
      if (i == elements.length - 1) {
        continue loop;
      }
      builder.append(',');
    }
  }

  @AfterTemplate
  void joiner(StringBuilder builder, Object[] elements) {
    Joiner.on(',').appendTo(builder, elements);
  }
}
