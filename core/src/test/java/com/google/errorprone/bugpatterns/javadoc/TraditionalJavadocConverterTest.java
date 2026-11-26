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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugpatterns.javadoc.TraditionalJavadocConverter.Replacement;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.Test;

public final class TraditionalJavadocConverterTest {

  @Test
  public void convertsParsedMethodCommentWithoutAttribution() throws IOException {
    String source =
        """
        class T {
          /** Returns {@code x}. */
          MissingType m() { return null; }
        }
        """;

    Replacement replacement = convertFirstMethod(source).orElseThrow();

    assertThat(source.substring(replacement.commentStart(), replacement.commentEnd()))
        .isEqualTo("/** Returns {@code x}. */");
    assertThat(apply(source, replacement))
        .isEqualTo(
            """
            class T {
              /// Returns `x`.
              MissingType m() { return null; }
            }
            """);
  }

  @Test
  public void handlesSameLineCodeBeforeAndAfterComment() throws IOException {
    String source =
        """
        class T {
          int x; /** Doc. */ void m() {}
        }
        """;

    Replacement replacement = convertFirstMethod(source).orElseThrow();

    assertThat(apply(source, replacement))
        .isEqualTo(
            """
            class T {
              int x;
              /// Doc.
              void m() {}
            }
            """);
  }

  private static String apply(String source, Replacement replacement) {
    return source.substring(0, replacement.start())
        + replacement.text()
        + source.substring(replacement.end());
  }

  @Test
  public void skipsDeprecatedWithoutAnnotation() throws IOException {
    String withoutAnnotation =
        """
        class T {
          /** @deprecated Use something else. */
          void m() {}
        }
        """;
    assertThat(convertFirstMethod(withoutAnnotation)).isEmpty();

    String withAnnotation =
        """
        class T {
          /** @deprecated Use something else. */
          @Deprecated
          void m() {}
        }
        """;
    assertThat(convertFirstMethod(withAnnotation)).isPresent();
  }

  @Test
  public void skipsCommentFollowingLineDocComment() throws IOException {
    String source =
        """
        class T {
          //////
          /** Doc. */
          void m() {}
        }
        """;
    assertThat(convertFirstMethod(source)).isEmpty();
  }

  private static Optional<Replacement> convertFirstMethod(String source) throws IOException {
    JavaCompiler compiler = requireNonNull(ToolProvider.getSystemJavaCompiler());
    JavaFileObject file =
        new SimpleJavaFileObject(URI.create("string:///T.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public String getCharContent(boolean ignoreEncodingErrors) {
            return source;
          }
        };
    JavacTask task =
        (JavacTask)
            compiler.getTask(null, null, null, ImmutableList.of(), null, ImmutableList.of(file));
    Iterable<? extends CompilationUnitTree> units = task.parse();
    DocTrees docTrees = DocTrees.instance(task);
    Replacement[] result = {null};
    for (CompilationUnitTree unit : units) {
      new TreePathScanner<Void, Void>() {
        @Override
        public Void visitMethod(MethodTree node, Void unused) {
          DocCommentTree docComment = docTrees.getDocCommentTree(getCurrentPath());
          if (docComment != null) {
            result[0] =
                TraditionalJavadocConverter.convert(unit, node, docComment, docTrees, source)
                    .orElse(null);
          }
          return null;
        }
      }.scan(new TreePath(unit), null);
    }
    return Optional.ofNullable(result[0]);
  }
}
