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

package com.google.errorprone.suppresswarnings;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneCompiler;
import com.google.errorprone.ErrorProneScanner;
import com.google.errorprone.ErrorProneScanner.EnabledPredicate;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.DeadException;
import com.google.errorprone.bugpatterns.EmptyIfStatement;
import com.google.errorprone.bugpatterns.SelfAssignment;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CustomSuppressionTest {
  private ErrorProneCompiler compiler;

  @Before
  public void setUp() {
    compiler = new ErrorProneCompiler.Builder()
        .report(new ErrorProneScanner(new EnabledPredicate() {
          @SuppressWarnings("unchecked")
          @Override
          public boolean isEnabled(Class<? extends BugChecker> check, BugPattern annotation) {
            return asList(DeadException.class, EmptyIfStatement.class, SelfAssignment.class)
                .contains(check);
          }
        }))
        .build();
  }

  @Test
  public void testNegativeCase() throws Exception {
    File source = new File(this.getClass().getResource("NegativeCases.java").toURI());
    assertThat(compiler.compile(new String[]{source.getAbsolutePath()}), is(0));
  }
}
