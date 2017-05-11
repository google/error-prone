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
import com.google.errorprone.refaster.annotation.Repeated;

/**
 * Example Refaster rule matching varargs.
 *
 * @author juanch@google.com (Juan Chen)
 */
public class VarargTemplate {
  @BeforeTemplate
  public String before(String template, @Repeated Object vararg) {
    return String.format(template, new Object[] {vararg});
  }

  @AfterTemplate
  public String after(String template, @Repeated Object vararg) {
    return String.format(template, vararg);
  }
}
