/*
 * Copyright 2021 The Error Prone Authors.
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

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class JUnitParameterMethodNotFoundPositiveCase {

  @Test
  @Parameters
  // BUG: Diagnostic contains: [JUnitParameterMethodNotFound]
  public void paramsInDefaultMethod(String p1, Integer p2) {}

  @Test
  @Parameters(method = "named2,named3")
  // BUG: Diagnostic contains: [JUnitParameterMethodNotFound]
  public void paramsInMultipleMethods(String p1, Integer p2) {}

  @Test
  @Parameters(source = JUnitParameterMethodNotFoundPositiveCase.class, method = "dataProvider")
  // BUG: Diagnostic contains: [JUnitParameterMethodNotFound]
  public void testSource(int a) {}

  static class Inner {
    public Object dataProvider() {
      return new Object[] {1};
    }
  }
}
