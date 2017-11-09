/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static org.junit.Assert.assertTrue;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author deminguyen@google.com (Demi Nguyen) */
@RunWith(JUnit4.class)
public class NonNullLiteralTest extends CompilerBasedAbstractTest {

  @Test
  public void shouldMatchPrimitiveLiterals() {
    writeFile(
        "A.java",
        "public class A {",
        "  public int getInt() {",
        "    return 2;",
        "  }",
        "  public long getLong() {",
        "    return 3L;",
        "  }",
        "  public float getFloat() {",
        "    return 4.0f;",
        "  }",
        "  public double getDouble() {",
        "    return 5.0d;",
        "  }",
        "  public boolean getBool() {",
        "    return true;",
        "  }",
        "  public char getChar() {",
        "    return 'c';",
        "  }",
        "  public String getString() {",
        "    return \"test string\";",
        "  }",
        "}");
    assertCompiles(nonNullLiteralMatches(/* shouldMatch= */ true, Matchers.nonNullLiteral()));
  }

  @Test
  public void shouldMatchClassLiteral() {
    writeFile(
        "A.java",
        "import java.lang.reflect.Type;",
        "public class A {",
        "  public void testClassLiteral() {",
        "    Type klass = String.class;",
        "  }",
        "}");
    assertCompiles(nonNullLiteralMatches(/* shouldMatch= */ true, Matchers.nonNullLiteral()));
  }

  @Test
  public void shouldNotMatchClassDeclaration() {
    writeFile(
        "A.java",
        "public class A {",
        "  protected class B {",
        "    private class C {",
        "    }",
        "  }",
        "}");
    assertCompiles(nonNullLiteralMatches(/* shouldMatch= */ false, Matchers.nonNullLiteral()));
  }

  @Test
  public void shouldNotMatchMemberAccess() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  public String stringVar;",
        "  public void testMemberAccess() {",
        "    this.stringVar = new String();",
        "  }",
        "}");

    writeFile(
        "B.java",
        "import com.google.A;",
        "public class B {",
        "  public void testInstanceMemberAccess() {",
        "    A foo = new A();",
        "    foo.stringVar = new String();",
        "  }",
        "}");
    assertCompiles(nonNullLiteralMatches(/* shouldMatch= */ false, Matchers.nonNullLiteral()));
  }

  private Scanner nonNullLiteralMatches(
      final boolean shouldMatch, final Matcher<ExpressionTree> toMatch) {
    return new Scanner() {
      @Override
      public Void visitLiteral(LiteralTree node, VisitorState visitorState) {
        assertTrue(node.toString(), !shouldMatch ^ toMatch.matches(node, visitorState));
        return super.visitLiteral(node, visitorState);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree node, VisitorState visitorState) {
        assertTrue(node.toString(), !shouldMatch ^ toMatch.matches(node, visitorState));
        return super.visitMemberSelect(node, visitorState);
      }

      @Override
      public Void visitImport(ImportTree node, VisitorState visitorState) {
        return null;
      }

      @Override
      public Void visitCompilationUnit(CompilationUnitTree node, VisitorState visitorState) {
        return null;
      }
    };
  }
}
