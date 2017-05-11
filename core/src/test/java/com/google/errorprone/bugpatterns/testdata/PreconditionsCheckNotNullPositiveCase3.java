/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Test case for static import of Precondtions.checkNotNull.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PreconditionsCheckNotNullPositiveCase3 {
  public void error() {
    // BUG: Diagnostic contains: remove this line
    checkNotNull("string literal");
  }
}
