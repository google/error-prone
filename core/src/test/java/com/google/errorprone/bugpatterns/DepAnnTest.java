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

package com.google.errorprone.bugpatterns;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;

public class DepAnnTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = new CompilationTestHelper(DepAnn.class);
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        new File(this.getClass().getResource("DepAnnPositiveCases.java").toURI()));
  }

  @Test
  public void testNegativeCase1() throws Exception {
    compilationHelper.assertCompileSucceeds(
        new File(this.getClass().getResource("DepAnnNegativeCase1.java").toURI()));
  }

  @Test
  @Ignore("blocked on javac7 bug")
  public void testNegativeCase2() throws Exception {
    compilationHelper.assertCompileSucceeds(
        new File(this.getClass().getResource("DepAnnNegativeCase2.java").toURI()));
  }

  @Test
  public void testDisableable() throws Exception {
    compilationHelper.assertCompileSucceeds(
        ImmutableList.of(new File(this.getClass().getResource("DepAnnPositiveCases.java").toURI())),
        "-Xepdisable:DepAnn");
  }
}
