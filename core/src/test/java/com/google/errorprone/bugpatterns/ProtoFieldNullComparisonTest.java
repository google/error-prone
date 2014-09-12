/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * @author flx@google.com (Felix Berger)
 */
@RunWith(JUnit4.class)
public class ProtoFieldNullComparisonTest {

  private CompilationTestHelper compilationHelper;
  private JavaFileObject protoFile;

  @Before
  public void setUp() throws Exception {
    compilationHelper = CompilationTestHelper.newInstance(new ProtoFieldNullComparison());
    protoFile = compilationHelper.fileManager().source(getClass(), "proto/ProtoTest.java");
  }

  private List<JavaFileObject> getSourceFiles(String mainFileName) throws URISyntaxException {
    JavaFileObject mainFile = compilationHelper.fileManager().source(getClass(), mainFileName);
    return ImmutableList.of(mainFile, protoFile);
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        getSourceFiles("ProtoFieldNullComparisonPositiveCases.java"));
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(
        getSourceFiles("ProtoFieldNullComparisonNegativeCases.java"));
  }

}
