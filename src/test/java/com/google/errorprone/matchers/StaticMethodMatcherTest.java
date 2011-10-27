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

package com.google.errorprone.matchers;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.ErrorFindingCompiler;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.ErrorChecker.AstError;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.junit.Assert.assertTrue;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class StaticMethodMatcherTest {

  @Rule public TestName name = new TestName();

  @Test
  public void shouldMatchUsingImportStatements() throws IOException {
    assertMatch(true, new String[]{
      "import com.google.A;",
      "public class " + name.getMethodName() + " {",
      "  public int count() {",
      "    return A.count();",
      "  }",
      "}",
    }, new StaticMethodMatcher("com.google", "A", "count"));
  }

  @Test
  public void shouldMatchFullyQualifiedCallSite() throws IOException {
    assertMatch(true, new String[]{
      "public class " + name.getMethodName() + " {",
      "  public int count() {",
      "    return com.google.A.count();",
      "  }",
      "}",
    }, new StaticMethodMatcher("com.google", "A", "count"));
  }

  @Test
  public void shouldNotMatchWhenPackageDiffers() throws IOException {
    assertMatch(false, new String[]{
        "import com.android.A;",
        "public class " + name.getMethodName() + " {",
        "  public int count() {",
        "    return A.count();",
        "  }",
        "}",
    }, new StaticMethodMatcher("com.google", "A", "count"));
  }

  private void assertMatch(final boolean shouldMatch, String[] sourceLines,
                           final StaticMethodMatcher staticMethodMatcher) throws IOException {
    File source = new File(System.getProperty("java.io.tmpdir"), name.getMethodName() + ".java");
    source.deleteOnExit();
    PrintWriter writer = new PrintWriter(new FileWriter(source));
    for (String sourceLine : sourceLines) {
      writer.println(sourceLine);
    }
    writer.close();


    new ErrorFindingCompiler(
        new String[]{ source.getAbsolutePath() },
        new DiagnosticCollector<JavaFileObject>(),
        getSystemJavaCompiler())
        .run(new ErrorCollectingTreeScanner() {
          @Override
          public List<AstError> visitImport(ImportTree importTree, VisitorState state) {
            state.imports.add(importTree);
            super.visitImport(importTree, state);
            return null;
          }

          @Override
          public List<AstError> visitMemberSelect(MemberSelectTree node, VisitorState visitorState)
          {
            if (getCurrentPath().getParentPath().getLeaf().getKind() == Kind.METHOD_INVOCATION) {
              assertTrue(node.toString(),
                  !shouldMatch ^ staticMethodMatcher.matches(node, visitorState));
            }
            return super.visitMemberSelect(node, visitorState);
          }
        });
  }
}
