/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.errorprone.fixes.ErrorProneEndPosTable;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ErrorProneParserTest {

  private static final String SOURCE = "/** javadoc */ class Test {}";

  @Test
  public void keepDocComments() {
    Context context = new Context();
    var unused = new JavacFileManager(context, true, UTF_8);
    JavacParser parser =
        ErrorProneParser.newParser(
            context,
            SOURCE,
            /* keepDocComments= */ true,
            /* keepEndPos= */ false,
            /* keepLineMap= */ false);
    JCCompilationUnit unit = parser.parseCompilationUnit();
    assertThat(unit.docComments).isNotNull();
    assertThat(unit.getLineMap()).isNull();
  }

  @Test
  public void discardDocComments() {
    Context context = new Context();
    var unused = new JavacFileManager(context, true, UTF_8);
    JavacParser parser =
        ErrorProneParser.newParser(
            context,
            SOURCE,
            /* keepDocComments= */ false,
            /* keepEndPos= */ false,
            /* keepLineMap= */ false);
    JCCompilationUnit unit = parser.parseCompilationUnit();
    assertThat(unit.docComments).isNull();
    assertThat(unit.getLineMap()).isNull();
  }

  @Test
  public void keepEndPos() {
    Context context = new Context();
    var unused = new JavacFileManager(context, true, UTF_8);
    JavacParser parser =
        ErrorProneParser.newParser(
            context,
            SOURCE,
            /* keepDocComments= */ false,
            /* keepEndPos= */ true,
            /* keepLineMap= */ false);
    JCCompilationUnit unit = parser.parseCompilationUnit();
    assertThat(unit.docComments).isNull();
    assertThat(ErrorProneEndPosTable.create(unit).getEndPosition(unit)).isEqualTo(28);
    assertThat(unit.getLineMap()).isNull();
  }

  @Test
  public void keepLineMap() {
    Context context = new Context();
    var unused = new JavacFileManager(context, true, UTF_8);
    JavacParser parser =
        ErrorProneParser.newParser(
            context,
            SOURCE,
            /* keepDocComments= */ false,
            /* keepEndPos= */ false,
            /* keepLineMap= */ true);
    JCCompilationUnit unit = parser.parseCompilationUnit();
    assertThat(unit.docComments).isNull();
    assertThat(unit.getLineMap()).isNotNull();
  }

  @Test
  public void discardLineMap() {
    Context context = new Context();
    var unused = new JavacFileManager(context, true, UTF_8);
    JavacParser parser =
        ErrorProneParser.newParser(
            context,
            SOURCE,
            /* keepDocComments= */ false,
            /* keepEndPos= */ false,
            /* keepLineMap= */ false);
    JCCompilationUnit unit = parser.parseCompilationUnit();
    assertThat(unit.docComments).isNull();
    assertThat(unit.getLineMap()).isNull();
  }
}
