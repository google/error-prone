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

package com.google.errorprone.suppress;

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.ErrorProneTestCompiler;
import com.google.errorprone.bugpatterns.DeadException;
import com.google.errorprone.bugpatterns.EmptyIfStatement;
import com.google.errorprone.bugpatterns.SelfAssignment;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.tools.javac.main.Main.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import javax.tools.JavaFileObject;

/** Tests for {@code @SuppressLint} suppression method. */
@RunWith(JUnit4.class)
public class SuppressLintTest {
  private ErrorProneTestCompiler compiler;

  @Before
  public void setUp() {
    ScannerSupplier scannerSupplier =
        ScannerSupplier.fromBugCheckerClasses(
            DeadException.class, EmptyIfStatement.class, SelfAssignment.class);
    compiler = new ErrorProneTestCompiler.Builder().report(scannerSupplier).build();
  }

  @Test
  public void testNegativeCase() throws Exception {
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "SuppressLintNegativeCases.java");
    assertThat(compiler.compile(sources)).isEqualTo(Result.OK);
  }
}
