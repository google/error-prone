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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class AppliedFixTest {

  final EndPosTable endPositions =
      new EndPosTable() {

        final Map<JCTree, Integer> map = new HashMap<>();

        @Override
        public void storeEnd(JCTree tree, int endpos) {
          map.put(tree, endpos);
        }

        @Override
        public int replaceTree(JCTree oldtree, JCTree newtree) {
          Integer endpos = map.getOrDefault(oldtree, Position.NOPOS);
          map.put(newtree, endpos);
          return endpos;
        }

        @Override
        public int getEndPos(JCTree tree) {
          Integer result = map.getOrDefault(tree, Position.NOPOS);
          return result;
        }
      };

  // TODO(b/67738557): consolidate helpers for creating fake trees
  JCTree node(int startPos, int endPos) {
    return new JCTree() {
      @Override
      public Tag getTag() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void accept(Visitor v) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> v, D d) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Kind getKind() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int getStartPosition() {
        return startPos;
      }

      @Override
      public int getEndPosition(EndPosTable endPosTable) {
        return endPos;
      }
    };
  }

  @Test
  public void shouldApplySingleFixOnALine() {
    JCTree node = node(11, 14);

    AppliedFix fix =
        AppliedFix.fromSource("import org.me.B;", endPositions).apply(SuggestedFix.delete(node));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("import org.B;"));
  }

  @Test
  public void shouldReportOnlyTheChangedLineInNewSnippet() {
    JCTree node = node(25, 26);

    AppliedFix fix =
        AppliedFix.fromSource("public class Foo {\n" + "  int 3;\n" + "}", endPositions)
            .apply(
                SuggestedFix.builder().prefixWith(node, "three").postfixWith(node, "tres").build());
    assertThat(fix.getNewCodeSnippet().toString()).isEqualTo("int three3tres;");
  }

  @Test
  public void shouldReturnNullOnEmptyFix() {
    AppliedFix fix =
        AppliedFix.fromSource("public class Foo {}", endPositions).apply(SuggestedFix.emptyFix());
    assertThat(fix).isNull();
  }

  @Test
  public void shouldReturnNullOnImportOnlyFix() {
    AppliedFix fix =
        AppliedFix.fromSource("public class Foo {}", endPositions)
            .apply(SuggestedFix.builder().addImport("foo.bar.Baz").build());
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
    JCTree node = node(21, 42);

    AppliedFix fix =
        AppliedFix.fromSource("package com.example;\n" + "import java.util.Map;\n", endPositions)
            .apply(SuggestedFix.delete(node));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("to remove this line"));
  }

  @Test
  public void shouldApplyFixesInReverseOrder() {
    // Have to use a mock Fix here in order to intentionally return Replacements in wrong order.
    Set<Replacement> replacements = new LinkedHashSet<>();
    replacements.add(Replacement.create(0, 1, ""));
    replacements.add(Replacement.create(1, 1, ""));

    Fix mockFix = mock(Fix.class);
    when(mockFix.getReplacements(any())).thenReturn(replacements);

    // If the fixes had been applied in the wrong order, this would fail.
    // But it succeeds, so they were applied in the right order.
    AppliedFix.fromSource(" ", endPositions).apply(mockFix);
  }

  @Test
  public void shouldThrowIfReplacementOutsideSource() {
    AppliedFix.Applier applier = AppliedFix.fromSource("Hello", endPositions);
    SuggestedFix fix = SuggestedFix.replace(0, 6, "World!");
    assertThrows(IllegalArgumentException.class, () -> applier.apply(fix));
  }
}
