/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;

/** @author rburny@google.com (Radoslaw Burny) */
public class JUnit3TestNotRunNegativeCase1 extends TestCase {

  // correctly spelled
  public void test() {}

  public void testCorrectlySpelled() {}

  // real words
  public void bestNameEver() {}

  public void destroy() {}

  public void restore() {}

  public void establish() {}

  public void estimate() {}

  // different signature
  public boolean teslaInventedLightbulb() {
    return true;
  }

  public void tesselate(float f) {}

  // surrounding class is not a JUnit3 TestCase
  private static class TestCase {
    private void tesHelper() {}

    private void destroy() {}
  }

  // correct test, despite redundant annotation
  @Test
  public void testILikeAnnotations() {}

  // both @Test & @Ignore
  @Test
  @Ignore
  public void ignoredTest2() {}

  @Ignore
  @Test
  public void ignoredTest() {}
}
