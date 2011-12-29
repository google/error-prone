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

import static com.google.common.io.Files.deleteRecursively;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.ErrorFindingCompiler;
import com.google.errorprone.ErrorFindingCompiler.Builder;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.DescribingMatcher.MatchDescription;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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

    writeFile("A.java",
        "package com.google;",
        "public class A { ",
        "  public static int count() {",
        "     return 1; ",
        "  }",
        "}"
    );
  }

  @After
  public void tearDown() throws Exception {
    deleteRecursively(tempDir);
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
        "public class B {",
        "  static class A {",
        "    public static int count() { return 0; }",
        "  }",
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
    ErrorCollectingTreeScanner scanner = new ErrorCollectingTreeScanner() {
      @Override
      public List<MatchDescription> visitMemberSelect(MemberSelectTree node, VisitorState visitorState) {
        if (getCurrentPath().getParentPath().getLeaf().getKind() == Kind.METHOD_INVOCATION) {
          assertTrue(node.toString(),
              !shouldMatch ^ staticMethodMatcher.matches(node, visitorState));
        }
        return super.visitMemberSelect(node, visitorState);
      }
    };
    ErrorFindingCompiler compiler = new Builder()
        .usingScanner(scanner)
        .build();

    File[] files = tempDir.listFiles();
    String[] args = new String[files.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = files[i].getAbsolutePath();
    }
    assertThat(compiler.compile(args), is(0));
  }
}
