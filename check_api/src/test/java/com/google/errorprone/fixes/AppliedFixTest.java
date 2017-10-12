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

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position;
import java.util.HashMap;
import java.util.Map;
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
          Integer endpos = map.get(oldtree);
          if (endpos == null) {
            endpos = Position.NOPOS;
          }
          map.put(newtree, endpos);
          return endpos;
        }

        @Override
        public int getEndPos(JCTree tree) {
          Integer result = map.get(tree);
          if (result == null) {
            result = Position.NOPOS;
          }
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
        AppliedFix.fromSource("public class Foo {}", endPositions)
            .apply(SuggestedFix.builder().build());
    assertNull(fix);
  }

  @Test
  public void shouldReturnNullOnImportOnlyFix() {
    AppliedFix fix =
        AppliedFix.fromSource("public class Foo {}", endPositions)
            .apply(SuggestedFix.builder().addImport("foo.bar.Baz").build());
    assertNull(fix);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionOnIllegalRange() {
    AppliedFix.fromSource("public class Foo {}", endPositions)
        .apply(SuggestedFix.replace(0, -1, ""));
  }

  @Test
  public void shouldSuggestToRemoveLastLineIfAsked() {
    JCTree node = node(21, 42);

    AppliedFix fix =
        AppliedFix.fromSource("package com.example;\n" + "import java.util.Map;\n", endPositions)
            .apply(SuggestedFix.delete(node));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("to remove this line"));
  }
}
