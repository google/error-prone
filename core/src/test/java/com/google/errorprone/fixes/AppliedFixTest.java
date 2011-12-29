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

import com.google.errorprone.checkers.ErrorChecker.Position;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class AppliedFixTest {
  @Test
  public void shouldApplySingleFixOnALine() {
    AppliedFix fix = AppliedFix.fromSource("import org.me.B;")
        .apply(SuggestedFix.delete(new Position(11, 14, null)));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("import org.B;"));
  }

  @Test
  public void shouldReportOnlyTheChangedLineInNewSnippet() {
    AppliedFix fix = AppliedFix.fromSource(
        "public class Foo {\n" +
        "  int 3;\n" +
        "}").apply(SuggestedFix.prefixWith(new Position(25, 26, null), "three"));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("int three3;"));
  }
}
