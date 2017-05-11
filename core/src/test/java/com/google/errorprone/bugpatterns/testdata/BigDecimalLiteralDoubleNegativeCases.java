/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import java.math.BigDecimal;

/** @author endobson@google.com (Eric Dobson) */
public class BigDecimalLiteralDoubleNegativeCases {

  public void foo() {
    new BigDecimal("99");
    new BigDecimal("99.0");
    new BigDecimal(123_459);
    new BigDecimal(123_456L);
    BigDecimal.valueOf(123);
    BigDecimal.valueOf(123L);
  }
}
