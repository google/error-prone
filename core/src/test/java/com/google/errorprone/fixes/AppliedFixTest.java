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
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(MockitoJUnitRunner.class)
public class AppliedFixTest {
  @Mock JCTree node;
  final EndPosTable endPositions = new EndPosTable() {
    
    Map<JCTree, Integer> map = new HashMap<>();
    
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
  
  @Test
  public void shouldApplySingleFixOnALine() {
    when(node.getStartPosition()).thenReturn(11);
    when(node.getEndPosition(same(endPositions))).thenReturn(14);

    AppliedFix fix = AppliedFix.fromSource("import org.me.B;", endPositions)
        .apply(SuggestedFix.delete(node));
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("import org.B;"));
  }

  @Test
  public void shouldReportOnlyTheChangedLineInNewSnippet() {
    when(node.getStartPosition()).thenReturn(25);
    when(node.getEndPosition(same(endPositions))).thenReturn(26);

    AppliedFix fix = AppliedFix.fromSource(
        "public class Foo {\n" +
        "  int 3;\n" +
        "}", endPositions)
        .apply(SuggestedFix.builder()
            .prefixWith(node, "three")
            .postfixWith(node, "tres")
            .build());
    assertThat(fix.getNewCodeSnippet().toString(), equalTo("int three3tres;"));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionOnEmptyFix() {
    AppliedFix.fromSource(
        "public class Foo {}", endPositions)
        .apply(SuggestedFix.builder().build());
  }

  @Test
  public void shouldReturnNullOnImportOnlyFix() {
    AppliedFix fix = AppliedFix.fromSource(
        "public class Foo {}", endPositions)
        .apply(SuggestedFix.builder().addImport("foo.bar.Baz").build());
    assertNull(fix);
  }
}
