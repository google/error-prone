/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Slightly funky test case with a custom After annotation)
 */
@RunWith(JUnit4.class)
public class JUnit4TearDownNotRunPositiveCaseCustomAfter {
  // This will compile-fail and suggest the import of org.junit.After
  // BUG: Diagnostic contains: @After
  @After public void tearDown() {}
}

@interface After {}
