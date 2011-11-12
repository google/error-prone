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

import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.junit.Assert.assertTrue;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.ErrorFindingCompiler;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.ErrorChecker.AstError;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class StaticMethodMatcherTest {

  @Rule public TestName name = new TestName();
  private File tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = new File(System.getProperty("java.io.tmpdir"),
        getClass().getCanonicalName() + "." + name.getMethodName());
    tempDir.mkdirs();
    tempDir.deleteOnExit();

    writeFile("A.java",
        "package com.google;",
        "public class A { ",
        "  public static int count() {",
        "     return 1; ",
        "  }",
        "}"
    );
  }

  @Test
  public void shouldMatchUsingImportStatements() throws IOException {
    writeFile("B.java",
      "import com.google.A;",
      "public class B {",
      "  public int count() {",
      "    return A.count();",
      "  }",
      "}"
    );
    assertMatch(true, new StaticMethodMatcher("com.google.A", "count"));
  }

  @Test
  public void shouldMatchFullyQualifiedCallSite() throws IOException {
    writeFile("B.java",
      "public class B {",
      "  public int count() {",
      "    return com.google.A.count();",
      "  }",
      "}"
    );
    assertMatch(true, new StaticMethodMatcher("com.google.A", "count"));
  }

  @Test
  public void shouldNotMatchWhenPackageDiffers() throws IOException {
    writeFile("B.java",
        "import com.android.A;",
        "public class B {",
        "  public int count() {",
        "    return A.count();",
        "  }",
        "}"
    );
    assertMatch(false, new StaticMethodMatcher("com.google.A", "count"));
  }

  private void writeFile(String fileName, String... lines) throws IOException {
    File source = new File(tempDir, fileName);
    PrintWriter writer = new PrintWriter(new FileWriter(source));
    for (String line : lines) {
      writer.println(line);
    }
    writer.close();
  }

  private void assertMatch(final boolean shouldMatch,
                           final StaticMethodMatcher staticMethodMatcher) throws IOException {
    new ErrorFindingCompiler(
        tempDir.list(),
        new DiagnosticCollector<JavaFileObject>(),
        getSystemJavaCompiler())
        .run(new ErrorCollectingTreeScanner() {
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
