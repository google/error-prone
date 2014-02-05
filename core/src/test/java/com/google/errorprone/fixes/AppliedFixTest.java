/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.fixes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.errorprone.ErrorProneEndPosMap;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(MockitoJUnitRunner.class)
public class AppliedFixTest {
  @Mock JCTree node;
  @Mock ErrorProneEndPosMap endPositions;

  @Test
  public void shouldApplySingleFixOnALine() {
    when(node.getStartPosition()).thenReturn(11);
    when(endPositions.getEndPosition(any(DiagnosticPosition.class))).thenReturn(14);

    AppliedFix fix = AppliedFix.fromSource("import org.me.B;", endPositions)
        .apply(new SuggestedFix().delete(node));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("import org.B;"));
  }

  @Test
  public void shouldReportOnlyTheChangedLineInNewSnippet() {
    when(node.getStartPosition()).thenReturn(25);
    when(endPositions.getEndPosition(any(DiagnosticPosition.class))).thenReturn(26);

    AppliedFix fix = AppliedFix.fromSource(
        "public class Foo {\n" +
        "  int 3;\n" +
        "}", endPositions)
        .apply(new SuggestedFix().prefixWith(node, "three").postfixWith(node, "tres"));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("int three3tres;"));
  }

  @Test
  public void shouldReturnNullOnEmptyFix() {
    AppliedFix fix = AppliedFix.fromSource(
        "public class Foo {}", endPositions)
        .apply(new SuggestedFix());
    assertNull(fix);
  }

  @Test
  public void shouldReturnNullOnImportOnlyFix() {
    AppliedFix fix = AppliedFix.fromSource(
        "public class Foo {}", endPositions)
        .apply(new SuggestedFix().addImport("foo.bar.Baz"));
    assertNull(fix);
  }
}
