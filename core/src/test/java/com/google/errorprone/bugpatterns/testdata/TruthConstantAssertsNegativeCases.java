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

import static com.google.common.truth.Truth.assertThat;

/**
 * Negative test cases for TruthConstantAsserts check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TruthConstantAssertsNegativeCases {

  public void testNegativeCases() {
    assertThat(new TruthConstantAssertsNegativeCases()).isEqualTo(Boolean.TRUE);
    assertThat(getObject()).isEqualTo(Boolean.TRUE);

    // assertion called on constant with constant expectation is ignored.
    assertThat(Boolean.FALSE).isEqualTo(4.2);
  }

  private static TruthConstantAssertsNegativeCases getObject() {
    return new TruthConstantAssertsNegativeCases();
  }
}
