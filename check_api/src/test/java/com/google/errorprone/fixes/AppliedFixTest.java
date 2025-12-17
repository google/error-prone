/*
 * Copyright 2011 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.util.Position;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class AppliedFixTest {

  // This is unused by the test, it just needs to be non-null.
  final ErrorProneEndPosTable endPositions = tree -> Position.NOPOS;

  ErrorPronePosition node(int startPos, int endPos) {
    return new FixedPosition(null, startPos, endPos);
  }

  @Test
  public void shouldApplySingleFixOnALine() {
    ErrorPronePosition node = node(11, 14);

    AppliedFix fix = AppliedFix.apply("import org.me.B;", endPositions, SuggestedFix.delete(node));
    assertThat(fix.snippet()).isEqualTo("import org.B;");
  }

  @Test
  public void shouldReportOnlyTheChangedLineInNewSnippet() {
    ErrorPronePosition node = node(25, 26);

    AppliedFix fix =
        AppliedFix.apply(
            """
            public class Foo {
              int 3;
            }\
            """,
            endPositions,
            SuggestedFix.builder().prefixWith(node, "three").postfixWith(node, "tres").build());
    assertThat(fix.snippet()).isEqualTo("int three3tres;");
  }

  @Test
  public void shouldReturnNullOnEmptyFix() {
    AppliedFix fix = AppliedFix.apply("public class Foo {}", endPositions, SuggestedFix.emptyFix());
    assertThat(fix).isNull();
  }

  @Test
  public void shouldReturnNullOnImportOnlyFix() {
    AppliedFix fix =
        AppliedFix.apply(
            "public class Foo {}",
            endPositions,
            SuggestedFix.builder().addImport("foo.bar.Baz").build());
    assertThat(fix).isNull();
  }

  @Test
  public void shouldThrowExceptionOnIllegalRange() {
    assertThrows(IllegalArgumentException.class, () -> SuggestedFix.replace(0, -1, ""));
    assertThrows(IllegalArgumentException.class, () -> SuggestedFix.replace(-1, -1, ""));
    assertThrows(IllegalArgumentException.class, () -> SuggestedFix.replace(-1, 1, ""));
  }

  @Test
  public void shouldSuggestToRemoveLastLineIfAsked() {
    ErrorPronePosition node = node(21, 42);

    AppliedFix fix =
        AppliedFix.apply(
            """
            package com.example;
            import java.util.Map;
            """,
            endPositions,
            SuggestedFix.delete(node));
    assertThat(fix.snippet()).isEqualTo("to remove this line");
  }

  @Test
  public void shouldApplyFixesInReverseOrder() {
    // Have to use a mock Fix here in order to intentionally return Replacements in wrong order.
    ImmutableSet<Replacement> replacements =
        ImmutableSet.of(Replacement.create(0, 1, ""), Replacement.create(1, 1, ""));

    Fix mockFix = mock(Fix.class);
    when(mockFix.getReplacements(ArgumentMatchers.<ErrorProneEndPosTable>any()))
        .thenReturn(replacements);

    // If the fixes had been applied in the wrong order, this would fail.
    // But it succeeds, so they were applied in the right order.
    var unused = AppliedFix.apply(" ", endPositions, mockFix);
  }

  @Test
  public void shouldThrowIfReplacementOutsideSource() {
    SuggestedFix fix = SuggestedFix.replace(0, 6, "World!");
    assertThrows(
        StringIndexOutOfBoundsException.class, () -> AppliedFix.apply("Hello", endPositions, fix));
  }
}
